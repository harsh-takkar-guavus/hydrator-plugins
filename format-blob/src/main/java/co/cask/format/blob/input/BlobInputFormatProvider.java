/*
 * Copyright © 2018 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.format.blob.input;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Name;
import co.cask.cdap.api.annotation.Plugin;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.api.plugin.PluginClass;
import co.cask.hydrator.format.input.PathTrackingConfig;
import co.cask.hydrator.format.input.PathTrackingInputFormatProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the entire contents of a File into a single record
 */
@Plugin(type = "inputformat")
@Name(BlobInputFormatProvider.NAME)
@Description(BlobInputFormatProvider.DESC)
public class BlobInputFormatProvider extends PathTrackingInputFormatProvider<BlobInputFormatProvider.BlobConfig> {
  static final String NAME = "blob";
  static final String DESC = "Plugin for reading files in blob format.";
  public static final PluginClass PLUGIN_CLASS =
    new PluginClass("inputformat", NAME, DESC, BlobInputFormatProvider.class.getName(),
                    "conf", PathTrackingConfig.FIELDS);

  public BlobInputFormatProvider(BlobConfig conf) {
    super(conf);
  }

  @Override
  public String getInputFormatClassName() {
    return PathTrackingBlobInputFormat.class.getName();
  }

  @Override
  protected void validate() {
    if (conf.containsMacro("schema")) {
      return;
    }

    Schema schema = conf.getSchema();
    String pathField = conf.getPathField();
    Schema.Field bodyField = schema.getField("body");
    if (bodyField == null) {
      throw new IllegalArgumentException("The schema for the 'blob' format must have a field named 'body'");
    }
    Schema bodySchema = bodyField.getSchema();
    Schema.Type bodyType = bodySchema.isNullable() ? bodySchema.getNonNullable().getType() : bodySchema.getType();
    if (bodyType != Schema.Type.BYTES) {
      throw new IllegalArgumentException(String.format("The 'body' field must be of type 'bytes', but found '%s'",
                                                       bodyType.name().toLowerCase()));
    }

    // blob must contain 'body' as type 'bytes'.
    // it can optionally contain a path field of type 'string'
    int numExpectedFields = pathField == null ? 1 : 2;
    int numFields = schema.getFields().size();
    if (numFields > numExpectedFields) {
      int numExtra = numFields - numExpectedFields;
      if (pathField == null) {
        throw new IllegalArgumentException(
          String.format("The schema for the 'blob' format must only contain the 'body' field, "
                          + "but found %d other field%s.", numFields - 1, numExtra > 1 ? "s" : ""));
      } else {
        throw new IllegalArgumentException(
          String.format("The schema for the 'blob' format must only contain the 'body' field and the '%s' field, "
                          + "but found %d other field%s.", pathField, numFields - 2, numExtra > 1 ? "s" : ""));
      }
    }
  }

  /**
   * Config for blob format. Overrides getSchema method to return the default schema if it is not provided.
   */
  public static class BlobConfig extends PathTrackingConfig {

    /**
     * Return the configured schema, or the default schema if none was given. Should never be called if the
     * schema contains a macro
     */
    @Override
    public Schema getSchema() {
      if (containsMacro("schema")) {
        throw new IllegalStateException("schema should not be checked until macros are evaluated.");
      }
      if (schema == null) {
        return getDefaultSchema();
      }
      try {
        return Schema.parseJson(schema);
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to parse schema: " + e.getMessage(), e);
      }
    }

    private Schema getDefaultSchema() {
      List<Schema.Field> fields = new ArrayList<>();
      fields.add(Schema.Field.of("body", Schema.of(Schema.Type.BYTES)));
      if (pathField != null && !pathField.isEmpty()) {
        fields.add(Schema.Field.of(pathField, Schema.of(Schema.Type.STRING)));
      }
      return Schema.recordOf("blob", fields);
    }
  }
}
