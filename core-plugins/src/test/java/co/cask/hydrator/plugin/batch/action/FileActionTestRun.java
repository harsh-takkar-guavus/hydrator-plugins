/*
 * Copyright © 2016 Cask Data, Inc.
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

package co.cask.hydrator.plugin.batch.action;

import co.cask.cdap.etl.api.action.Action;
import co.cask.cdap.etl.proto.v2.ETLBatchConfig;
import co.cask.cdap.etl.proto.v2.ETLPlugin;
import co.cask.cdap.etl.proto.v2.ETLStage;
import co.cask.cdap.test.ApplicationManager;
import co.cask.hydrator.plugin.batch.ETLBatchTestBase;
import com.google.common.collect.ImmutableMap;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * Test for HDFSAction.
 */
public class FileActionTestRun extends ETLBatchTestBase {
  @ClassRule
  public static TemporaryFolder folder = new TemporaryFolder();

  private static MiniDFSCluster dfsCluster;
  private static FileSystem fileSystem;

  @BeforeClass
  public static void buildMiniDFS() throws Exception {
    // Setup MiniDFSCluster
    File baseDir = folder.newFolder();
    org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
    MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
    dfsCluster = builder.build();
    dfsCluster.waitActive();
    fileSystem = FileSystem.get(conf);
  }

  @AfterClass
  public static void cleanupMiniDFS() {
    // Shutdown MiniDFSCluster
    dfsCluster.shutdown();
    folder.delete();
  }

  @Test
  public void testFailedHDFSAction() throws Exception {
    //expect action phase and therefore pipeline to fail because of invalid source path

    Path outputDir = dfsCluster.getFileSystem().getHomeDirectory();

    fileSystem.mkdirs(new Path("source"));
    fileSystem.createNewFile(new Path("source/test.txt"));
    fileSystem.createNewFile(new Path("source/test.json"));

    ETLStage action = new ETLStage(
      "FileMove",
      new ETLPlugin("FileMove", Action.PLUGIN_TYPE,
                    ImmutableMap.of("sourcePath", outputDir.toUri().toString() + "/test",
                                    "destPath", outputDir.toUri().toString() + "/",
                                    "fileRegex", ".*\\.txt",
                                    "continueOnError", "false"),
                    null));
    ETLBatchConfig etlConfig = ETLBatchConfig.builder("* * * * *")
      .addStage(action)
      .build();

    ApplicationManager appManager = deployETL(etlConfig, "hdfsFailedActionTest");
    runETLOnce(appManager);

    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/source/test.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/source/test.json")));
  }

  @Test
  public void testHDFSAction() throws Exception {
    //moves only test.txt from `/source` to `/dest`, which is also created during the Action process

    Path outputDir = dfsCluster.getFileSystem().getHomeDirectory();

    fileSystem.mkdirs(new Path("source"));
    fileSystem.createNewFile(new Path("source/test.txt"));
    fileSystem.createNewFile(new Path("source/test.json"));

    ETLStage action = new ETLStage(
      "FileMove",
      new ETLPlugin("FileMove", Action.PLUGIN_TYPE,
                    ImmutableMap.of("sourcePath", outputDir.toUri().toString() + "/source",
                                    "destPath", outputDir.toUri().toString() + "/dest",
                                    "fileRegex", ".*\\.txt",
                                    "continueOnError", "false"),
                    null));
    ETLBatchConfig etlConfig = ETLBatchConfig.builder("* * * * *")
      .addStage(action)
      .build();

    ApplicationManager appManager = deployETL(etlConfig, "hdfsSuccessActionTest");
    runETLOnce(appManager);

    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/dest/test.txt")));
    Assert.assertFalse(fileSystem.exists(new Path(outputDir.toUri().toString() + "/dest/test.json")));
  }

  @Test
  public void testInvalidDestHDFSAction() throws Exception {
    //destPath is an existing text file, not a directory, so the pipeline should fail

    Path outputDir = dfsCluster.getFileSystem().getHomeDirectory();

    fileSystem.mkdirs(new Path("source"));
    fileSystem.createNewFile(new Path("source/test.txt"));
    fileSystem.createNewFile(new Path("source/test2.txt"));
    fileSystem.createNewFile(new Path("source/test.json"));
    fileSystem.createNewFile(new Path("dest/dest.txt"));

    ETLStage action = new ETLStage(
      "FileMove",
      new ETLPlugin("FileMove", Action.PLUGIN_TYPE,
                    ImmutableMap.of("sourcePath", outputDir.toUri().toString() + "/source",
                                    "destPath", outputDir.toUri().toString() + "/dest/dest.txt",
                                    "fileRegex", ".*\\.txt",
                                    "continueOnError", "false"),
                    null));
    ETLBatchConfig etlConfig = ETLBatchConfig.builder("* * * * *")
      .addStage(action)
      .build();

    ApplicationManager appManager = deployETL(etlConfig, "hdfsInvalidActionTest");
    runETLOnce(appManager);

    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/source/test.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/source/test2.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/source/test.json")));
  }

  @Test
  public void testHDFSDeleteAction() throws Exception {

    Path outputDir = dfsCluster.getFileSystem().getHomeDirectory();

    fileSystem.mkdirs(new Path("basicDir"));
    fileSystem.createNewFile(new Path("basicDir/test.txt"));
    fileSystem.createNewFile(new Path("basicDir/test2.txt"));
    fileSystem.createNewFile(new Path("basicDir/test.json"));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/basicDir/test.json")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/basicDir/test.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/basicDir/test2.txt")));

    ETLStage action = new ETLStage(
      "FileDelete",
      new ETLPlugin("FileDelete", Action.PLUGIN_TYPE,
                    ImmutableMap.of("path", outputDir.toUri().toString() + "/basicDir",
                                    "fileRegex", ".*\\.txt",
                                    "continueOnError", "false"),
                    null));
    ETLBatchConfig etlConfig = ETLBatchConfig.builder("* * * * *")
      .addStage(action)
      .build();

    ApplicationManager appManager = deployETL(etlConfig, "hdfsDeleteActionTest");
    runETLOnce(appManager);

    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/basicDir/test.json")));
    Assert.assertFalse(fileSystem.exists(new Path(outputDir.toUri().toString() + "/basicDir/test.txt")));
    Assert.assertFalse(fileSystem.exists(new Path(outputDir.toUri().toString() + "/basicDir/test2.txt")));

  }

  @Test
  public void testHDFSDeleteSingleFileNoRegexAction() throws Exception {

    Path outputDir = dfsCluster.getFileSystem().getHomeDirectory();

    fileSystem.mkdirs(new Path("dir"));
    fileSystem.createNewFile(new Path("dir/test.txt"));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/dir/test.txt")));

    ETLStage action = new ETLStage(
      "FileDelete",
      new ETLPlugin("FileDelete", Action.PLUGIN_TYPE,
                    ImmutableMap.of("path", outputDir.toUri().toString() + "/dir/test.txt",
                                    "continueOnError", "false"),
                    null));
    ETLBatchConfig etlConfig = ETLBatchConfig.builder("* * * * *")
      .addStage(action)
      .build();

    ApplicationManager appManager = deployETL(etlConfig, "hdfsDeleteSingleFileNoRegexActionTest");
    runETLOnce(appManager);

    Assert.assertFalse(fileSystem.exists(new Path(outputDir.toUri().toString() + "/dir/test.txt")));
  }

  @Test
  public void testHDFSDeleteSingleFileAction() throws Exception {
    Path outputDir = dfsCluster.getFileSystem().getHomeDirectory();

    fileSystem.mkdirs(new Path("singleDir"));
    fileSystem.createNewFile(new Path("singleDir/test.txt"));
    fileSystem.createNewFile(new Path("singleDir/test2.txt"));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/singleDir/test.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/singleDir/test2.txt")));

    ETLStage action = new ETLStage(
      "FileDelete",
      new ETLPlugin("FileDelete", Action.PLUGIN_TYPE,
                    ImmutableMap.of("path", outputDir.toUri().toString() + "/singleDir/test.txt",
                                    "fileRegex", ".*\\.txt",
                                    "continueOnError", "false"),
                    null));
    ETLBatchConfig etlConfig = ETLBatchConfig.builder("* * * * *")
      .addStage(action)
      .build();

    ApplicationManager appManager = deployETL(etlConfig, "hdfsDeleteSingleFileActionTest");
    runETLOnce(appManager);

    Assert.assertFalse(fileSystem.exists(new Path(outputDir.toUri().toString() + "/singleDir/test.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/singleDir/test2.txt")));
  }

  @Test
  public void testHDFSDeleteDirectoryAction() throws Exception {
    Path outputDir = dfsCluster.getFileSystem().getHomeDirectory();

    fileSystem.mkdirs(new Path("dir1/source/dir2"));
    fileSystem.createNewFile(new Path("dir1/source/test.txt"));
    fileSystem.createNewFile(new Path("dir1/source/test2.txt"));
    fileSystem.createNewFile(new Path("dir1/source/dir2/test2.txt"));

    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/dir1/source/test.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/dir1/source/test2.txt")));
    Assert.assertTrue(fileSystem.exists(new Path(outputDir.toUri().toString() + "/dir1/source/dir2/test2.txt")));

    ETLStage action = new ETLStage(
      "FileDelete",
      new ETLPlugin("FileDelete", Action.PLUGIN_TYPE,
                    ImmutableMap.of("path", outputDir.toUri().toString() + "/dir1/source",
                                    "continueOnError", "false"),
                    null));
    ETLBatchConfig etlConfig = ETLBatchConfig.builder("* * * * *")
      .addStage(action)
      .build();

    ApplicationManager appManager = deployETL(etlConfig, "hdfsDeleteDirectoryActionTest");
    runETLOnce(appManager);

    Assert.assertFalse(fileSystem.isDirectory(new Path(outputDir.toUri().toString() + "/dir1/source")));
  }
}
