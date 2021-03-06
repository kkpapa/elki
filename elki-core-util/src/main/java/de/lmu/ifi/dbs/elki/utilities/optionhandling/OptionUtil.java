/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.DocumentationUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Utility functions related to Option handling.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @apiviz.uses Parameter
 */
public final class OptionUtil {
  /**
   * Fake constructor. Use static method.
   */
  private OptionUtil() {
    // Do not instantiate.
  }

  /**
   * Returns a string representation of the specified list of options containing
   * the names of the options.
   * 
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Parameter<?>> String optionsNamesToString(List<O> options) {
    StringBuilder buffer = new StringBuilder(1000).append('[');
    for(int i = 0; i < options.size(); i++) {
      buffer.append(options.get(i).getOptionID().getName());
      if(i != options.size() - 1) {
        buffer.append(',');
      }
    }
    return buffer.append(']').toString();
  }

  /**
   * Returns a string representation of the specified list of options containing
   * the names of the options.
   * 
   * @param <O> Option type
   * @param options the list of options
   * @return the names of the options
   */
  public static <O extends Parameter<?>> String optionsNamesToString(O[] options) {
    StringBuilder buffer = new StringBuilder(1000).append('[');
    for(int i = 0; i < options.length; i++) {
      buffer.append(options[i].getOptionID().getName());
      if(i != options.length - 1) {
        buffer.append(',');
      }
    }
    return buffer.append(']').toString();
  }

  /**
   * Returns a string representation of the list of number parameters containing
   * the names and the values of the parameters.
   * 
   * @param <N> Parameter type
   * @param parameters the list of number parameters
   * @return the names and the values of the parameters
   */
  public static <N extends Parameter<?>> String parameterNamesAndValuesToString(List<N> parameters) {
    StringBuilder buffer = new StringBuilder(1000).append('[');
    for(int i = 0; i < parameters.size(); i++) {
      buffer.append(parameters.get(i).getOptionID().getName()).append(':') //
          .append(parameters.get(i).getValueAsString());
      if(i != parameters.size() - 1) {
        buffer.append(", ");
      }
    }
    return buffer.append(']').toString();
  }

  /**
   * Format a list of options (and associated owning objects) for console help
   * output.
   * 
   * @param buf Serialization buffer
   * @param width Screen width
   * @param indent Indentation string
   * @param options List of options
   */
  public static void formatForConsole(StringBuilder buf, int width, String indent, Collection<TrackedParameter> options) {
    for(TrackedParameter pair : options) {
      println(buf//
          .append(SerializedParameterization.OPTION_PREFIX).append(pair.getParameter().getOptionID().getName()) //
          .append(' ').append(pair.getParameter().getSyntax()).append(FormatUtil.NEWLINE), //
          width, getFullDescription(pair.getParameter()), indent);
    }
  }

  /**
   * Format a parameter description.
   * 
   * @param param
   * @return Parameter description
   */
  public static <T> String getFullDescription(Parameter<T> param) {
    StringBuilder description = new StringBuilder(1000);
    // description.append(getParameterType()).append(" ");
    description.append(param.getShortDescription()).append(FormatUtil.NEWLINE);
    param.describeValues(description);
    if(!FormatUtil.endsWith(description, FormatUtil.NEWLINE)) {
      description.append(FormatUtil.NEWLINE);
    }
    if(param.hasDefaultValue()) {
      description.append("Default: ").append(param.getDefaultValueAsString()).append(FormatUtil.NEWLINE);
    }
    List<ParameterConstraint<? super T>> constraints = param.getConstraints();
    if(constraints != null && !constraints.isEmpty()) {
      description.append((constraints.size() == 1) ? "Constraint: " : "Constraints: ");
      for(int i = 0; i < constraints.size(); i++) {
        description.append(i > 0 ? ", " : "").append(constraints.get(i).getDescription(param.getOptionID().getName()));
      }
      description.append('.').append(FormatUtil.NEWLINE);
    }
    return description.toString();
  }

  /**
   * Simple writing helper with no indentation.
   * 
   * @param buf Buffer to write to
   * @param width Width to use for linewraps
   * @param data Data to write.
   * @param indent Indentation
   */
  private static void println(StringBuilder buf, int width, String data, String indent) {
    for(String line : FormatUtil.splitAtLastBlank(data, width - indent.length())) {
      buf.append(indent).append(line);
      if(!line.endsWith(FormatUtil.NEWLINE)) {
        buf.append(FormatUtil.NEWLINE);
      }
    }
  }

  /**
   * Format a description of a Parameterizable (including recursive options).
   * 
   * @param buf Buffer to append to.
   * @param pcls Parameterizable class to describe
   * @param width Width
   * @param indent Text indent
   * @return Formatted description
   */
  public static StringBuilder describeParameterizable(StringBuilder buf, Class<?> pcls, int width, String indent) {
    try {
      println(buf, width, "Description for class " + pcls.getName(), "");

      String title = DocumentationUtil.getTitle(pcls);
      if(title != null && title.length() > 0) {
        println(buf, width, title, "");
      }

      String desc = DocumentationUtil.getDescription(pcls);
      if(desc != null && desc.length() > 0) {
        println(buf, width, desc, "  ");
      }

      Reference ref = DocumentationUtil.getReference(pcls);
      if(ref != null) {
        if(ref.prefix().length() > 0) {
          println(buf, width, ref.prefix(), "");
        }
        println(buf, width, ref.authors() + ":", "");
        println(buf, width, ref.title(), "  ");
        println(buf, width, "in: " + ref.booktitle(), "");
        if(ref.url().length() > 0) {
          println(buf, width, "see also: " + ref.url(), "");
        }
      }

      SerializedParameterization config = new SerializedParameterization();
      TrackParameters track = new TrackParameters(config);
      @SuppressWarnings("unused")
      Object p = ClassGenericsUtil.tryInstantiate(Object.class, pcls, track);
      Collection<TrackedParameter> options = track.getAllParameters();
      if(!options.isEmpty()) {
        OptionUtil.formatForConsole(buf, width, indent, options);
      }
      return buf;
    }
    catch(Exception e) {
      LoggingUtil.exception("Error instantiating class to describe.", e.getCause());
      return buf.append("No description available: ").append(e);
    }
  }
}
