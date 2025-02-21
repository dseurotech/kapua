###############################################################################
# Copyright (c) 2024, 2024 Eurotech and/or its affiliates and others
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Eurotech - initial API and implementation
###############################################################################
@env_docker
@it
@jobEngine
@jobEngineTargetProcessors

Feature: Job Engine Service - Bundle Step Processors
  Tests for Device Management Bundle Processor

  @setup
  Scenario: Setup test resources
    Given Init Security Context
    And Start Docker environment with resources
      | db                  |
      | events-broker       |
      | job-engine          |
      | message-broker      |
      | broker-auth-service |
      | consumer-lifecycle  |

  Scenario: Bundle Start

    Given I login as user with name "kapua-sys" and password "kapua-password"
    And I start the Kura Mock
    And Device birth message is sent
    And Device "rpione3" is connected within 10s
    And I create a job with the name "TestJob"
    And I add device targets to job
      | rpione3 |
    And I search for step definition with the name
      | Bundle Start |
    And I add job step to job with name "Test Step - Bundle Start" and with selected job step definition and properties
      | name     | type              | value |
      | bundleId | java.lang.String  | 34    |
      | timeout  | java.lang.Long    | 5000  |
    When I start a job
    And I wait job to finish its execution up to 10s
    Then I confirm that job has 1 job execution
    And I confirm that job target in job has step index 0 and status "PROCESS_OK"

  Scenario: Bundle Stop

    Given I login as user with name "kapua-sys" and password "kapua-password"
    And I start the Kura Mock
    And Device birth message is sent
    And Device "rpione3" is connected within 10s
    And I create a job with the name "TestJob"
    And I add device targets to job
      | rpione3 |
    And I search for step definition with the name
      | Bundle Stop |
    And I add job step to job with name "Test Step - Bundle Stop" and with selected job step definition and properties
      | name     | type              | value |
      | bundleId | java.lang.String  | 34    |
      | timeout  | java.lang.Long    | 5000  |
    When I start a job
    And I wait job to finish its execution up to 10s
    Then I confirm that job has 1 job execution
    And I confirm that job target in job has step index 0 and status "PROCESS_OK"

  @teardown
  Scenario: Tear down test resources
    Given I logout
    And KuraMock is disconnected
    And Stop Docker environment
    And Clean Locator Instance