package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * A precomputed neighborhood, loaded from an external file.
 * 
 * @author Erich Schubert
 */
public class ExternalNeighborhood extends AbstractPrecomputedNeighborhood implements Result {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(ExternalNeighborhood.class);

  /**
   * Parameter to specify the neighborhood file
   */
  public static final OptionID NEIGHBORHOOD_FILE_ID = OptionID.getOrCreateOptionID("externalneighbors.file", "The file listing the neighbors.");

  /**
   * Constructor.
   * 
   * @param store Store to access
   */
  public ExternalNeighborhood(DataStore<DBIDs> store) {
    super(store);
  }

  @Override
  public String getLongName() {
    return "External Neighborhood";
  }

  @Override
  public String getShortName() {
    return "external-neighborhood";
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has ExternalNeighborhood
   */
  public static class Factory extends AbstractPrecomputedNeighborhood.Factory<Object> {
    /**
     * Logger
     */
    private static final Logging logger = Logging.getLogger(ExternalNeighborhood.class);

    /**
     * The input file.
     */
    private File file;

    /**
     * Constructor.
     * 
     * @param file File to load
     */
    public Factory(File file) {
      super();
      this.file = file;
    }

    @Override
    public NeighborSetPredicate instantiate(Relation<?> database) {
      DataStore<DBIDs> store = loadNeighbors(database);
      ExternalNeighborhood neighborhood = new ExternalNeighborhood(store);
      ResultHierarchy hier = database.getHierarchy();
      if(hier != null) {
        hier.add(database, neighborhood);
      }
      return neighborhood;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.ANY;
    }

    /**
     * Method to load the external neighbors.
     */
    private DataStore<DBIDs> loadNeighbors(Relation<?> database) {
      final WritableDataStore<DBIDs> store = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);

      if(logger.isVerbose()) {
        logger.verbose("Loading external neighborhoods.");
      }

      if(logger.isDebugging()) {
        logger.verbose("Building reverse label index...");
      }
      // Build a map label/ExternalId -> DBID
      // (i.e. a reverse index!)
      // TODO: move this into the database layer to share?
      Map<String, DBID> lblmap = new HashMap<String, DBID>(database.size() * 2);
      {
        Relation<LabelList> olq = database.getDatabase().getRelation(SimpleTypeInformation.get(LabelList.class));
        Relation<String> eidq = null; // database.getExternalIdQuery();
        for(DBID id : database.iterDBIDs()) {
          if(eidq != null) {
            String eid = eidq.get(id);
            if(eid != null) {
              lblmap.put(eid, id);
            }
          }
          if(olq != null) {
            LabelList label = olq.get(id);
            if(label != null) {
              for(String lbl : label) {
                lblmap.put(lbl, id);
              }
            }
          }
        }
      }

      try {
        if(logger.isDebugging()) {
          logger.verbose("Loading neighborhood file.");
        }
        InputStream in = new FileInputStream(file);
        in = FileUtil.tryGzipInput(in);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        for(String line; (line = br.readLine()) != null; ) {
          ArrayModifiableDBIDs neighbours = DBIDUtil.newArray();
          String[] entries = line.split(" ");
          DBID id = lblmap.get(entries[0]);
          if(id != null) {
            for(int i = 0; i < entries.length; i++) {
              final DBID neigh = lblmap.get(entries[i]);
              if(neigh != null) {
                neighbours.add(neigh);
              }
              else {
                logger.warning("No object found for label " + entries[i]);
              }
            }
            store.put(id, neighbours);
          }
          else {
            logger.warning("No object found for label " + entries[0]);
          }
        }
        br.close();
        in.close();

        return store;
      }
      catch(IOException e) {
        throw new AbortException("Loading of external neighborhood failed.", e);
      }
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      /**
       * The input file.
       */
      File file;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        file = getParameterNeighborhoodFile(config);
      }

      /**
       * Get the neighborhood parameter.
       * 
       * @param config Parameterization
       * @return Instance or null
       */
      protected static File getParameterNeighborhoodFile(Parameterization config) {
        final FileParameter param = new FileParameter(NEIGHBORHOOD_FILE_ID, FileParameter.FileType.INPUT_FILE);
        if(config.grab(param)) {
          return param.getValue();
        }
        return null;
      }

      @Override
      protected ExternalNeighborhood.Factory makeInstance() {
        return new ExternalNeighborhood.Factory(file);
      }
    }
  }
}