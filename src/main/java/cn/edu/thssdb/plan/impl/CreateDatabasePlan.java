/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Manager;

public class CreateDatabasePlan extends LogicalPlan {

  private String databaseName;

  public CreateDatabasePlan(String databaseName) {
    super(LogicalPlanType.CREATE_DB);
    this.databaseName = databaseName;
  }
  /** [method] 执行操作 */
  @Override
  public void exec() {
    Manager manager = Manager.getInstance();
    manager.createDatabaseIfNotExists(databaseName);
  }

  @Override
  public String getMessage() {
    return null;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  @Override
  public String toString() {
    return "CreateDatabasePlan{" + "databaseName='" + databaseName + '\'' + '}';
  }
}
