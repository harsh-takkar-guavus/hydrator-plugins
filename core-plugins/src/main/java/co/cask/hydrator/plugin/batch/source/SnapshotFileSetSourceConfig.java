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

package co.cask.hydrator.plugin.batch.source;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Macro;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.hydrator.plugin.common.SnapshotFileSetConfig;

import java.io.IOException;
import javax.annotation.Nullable;

/**
 * Plugin config for snapshot sources, which always require a schema.
 */
public class SnapshotFileSetSourceConfig extends SnapshotFileSetConfig {

  @Macro
  @Description("Schema of data to read.")
  private String schema;

  @Nullable
  public Schema getSchema() {
    try {
      return containsMacro("schema") || schema == null ? null : Schema.parseJson(schema);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to parse schema: " + e.getMessage(), e);
    }
  }
}
