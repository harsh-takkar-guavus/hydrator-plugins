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

package co.cask.hydrator.format.output;

import co.cask.cdap.api.data.format.StructuredRecord;
import co.cask.cdap.api.dataset.lib.KeyValue;
import co.cask.cdap.format.StructuredRecordStringConverter;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobContext;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Defines logic for writing JSON files.
 */
public class JsonOutputFormatter implements FileOutputFormatter<NullWritable, Text> {

  @Override
  public KeyValue<NullWritable, Text> transform(StructuredRecord record) {
    try {
      return new KeyValue<>(NullWritable.get(),
                            new Text(StructuredRecordStringConverter.toJsonString(record)));
    } catch (IOException e) {
      throw new RuntimeException("Unable to transform CDAP record into a JSON record.", e);
    }
  }

  @Override
  public String getFormatClassName() {
    return TextOutputFormat.class.getName();
  }

  @Override
  public Map<String, String> getFormatConfig() {
    return Collections.singletonMap(JobContext.OUTPUT_KEY_CLASS, Text.class.getName());
  }
}
