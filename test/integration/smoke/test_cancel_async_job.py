# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Import Local Modules
from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (ServiceOffering,
                             Account,
                             VirtualMachine,
                             AsyncJob
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_volumes,
                               validateList
                               )
from marvin.codes import (PASS,
                          FAILED,
                          JOB_SUCCEEDED,
                          JOB_CANCELLED,
                          JOB_FAILED
                          )
from nose.plugins.attrib import attr
import time
from marvin.cloudstackException import CloudstackAPIException


class TestCancelJob(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCancelJob, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])

        cls._cleanup = []

        # Create Service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"],
        )
        cls._cleanup.append(cls.service_offering)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

        # Method to Deploy VM but will return immediately as an asynchronous
        # task does.
    @classmethod
    def deployVM(self, apiclient, services, templateid=None, accountid=None,
                 domainid=None, zoneid=None, networkids=None,
                 serviceofferingid=None, diskofferingid=None):
        """Deploy A VM  """
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.isAsync = "false"
        if serviceofferingid:
            cmd.serviceofferingid = serviceofferingid
        elif "serviceoffering" in services:
            cmd.serviceofferingid = services["serviceoffering"]

        if zoneid:
            cmd.zoneid = zoneid
        elif "zoneid" in services:
            cmd.zoneid = services["zoneid"]

        if "displayname" in services:
            cmd.displayname = services["displayname"]

        if "name" in services:
            cmd.name = services["name"]

        if accountid:
            cmd.account = accountid
        elif "account" in services:
            cmd.account = services["account"]

        if domainid:
            cmd.domainid = domainid
        elif "domainid" in services:
            cmd.domainid = services["domainid"]

        if networkids:
            cmd.networkids = networkids
            allow_egress = False
        elif "networkids" in services:
            cmd.networkids = services["networkids"]
            allow_egress = False
        else:
            # When no networkids are passed, network
            # is created using the "defaultOfferingWithSourceNAT"
            # which has an egress policy of DENY. But guests in tests
            # need access to test network connectivity
            allow_egress = True

        if templateid:
            cmd.templateid = templateid
        elif "template" in services:
            cmd.templateid = services["template"]

        if diskofferingid:
            cmd.diskofferingid = diskofferingid
        elif "diskoffering" in services:
            cmd.diskofferingid = services["diskoffering"]

        return apiclient.deployVirtualMachine(cmd)

# Method to check the volume attach async jobs' status
    def query_child_job_status(self, jobid):
        """Query the status for Async Job"""
        try:
            asyncTimeout = 3600
            timeout = asyncTimeout
            status = FAILED
            while timeout > 0:

                qresultset = self.dbclient.execute(
                    "select job_status from async_job where id = '%s';"
                    % jobid)
                list_validation_result = validateList(qresultset)
                self.assertEqual(
                    list_validation_result[0],
                    PASS,
                    "list validation failed due to %s" %
                    list_validation_result[2])

                qset = qresultset[0]
                job_status = qset[0]
                if job_status == 2 or job_status == 3:
                    status = PASS
                    break
                if job_status == 1:
                    status = FAILED
                    break
                    status = PASS
                    break

                time.sleep(5)
                timeout -= 5
                self.debug("=== JobId: %s is Still Processing, "
                           "Will TimeOut in: %s ====" % (str(jobid),
                                                         str(timeout)))
            return status
        except Exception as e:
            self.debug("==== Exception Occurred for Job: %s ====" %
                       str(e))
            return FAILED

    def verify_job_status(self, apiclient, jobid):
        """Query the status for Async Job"""
        try:

            cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
            cmd.jobid = jobid
            async_response = apiclient.queryAsyncJobResult(cmd)
            job_status = async_response.jobstatus
            self.assertEqual(
                job_status,
                3,
                " job expected status is JOB_CANCELLED but current job status is %s " %
                job_status)
            return job_status
        except Exception as e:
            self.debug("==== Exception Occurred for Job: %s ====" %
                       str(e))
            return FAILED

    def create_snapshot(self, apiclient, volume_id, account=None,
                        domainid=None, projectid=None):
        """Create Snapshot"""
        cmd = createSnapshot.createSnapshotCmd()
        cmd.isAsync = "false"
        cmd.volumeid = volume_id
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        if projectid:
            cmd.projectid = projectid
        try:
            return apiclient.createSnapshot(cmd)
        except Exception as e:
            raise Exception("Warning: Exception during Snapshot creation")

    def delete_snapshot(self, apiclient, volumeid):
        """Delete Snapshot"""
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = volumeid
        apiclient.deleteSnapshot(cmd)

    def query_async_job(self, apiclient, jobid):
        """Query the status for Async Job"""
        try:
            asyncTimeout = 3600
            cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
            cmd.jobid = jobid
            timeout = asyncTimeout
            async_response = FAILED
            while timeout > 0:
                async_response = apiclient.queryAsyncJobResult(cmd)
                if async_response != FAILED:
                    job_status = async_response.jobstatus
                    if job_status in [JOB_CANCELLED,
                                      JOB_SUCCEEDED]:
                        break
                    elif job_status == JOB_FAILED:
                        raise Exception("Job failed: %s"
                                        % async_response)
                time.sleep(5)
                timeout -= 5
                self.debug("=== JobId: %s is Still Processing, "
                           "Will TimeOut in: %s ====" % (str(jobid),
                                                         str(timeout)))
            return async_response
        except Exception as e:
            self.debug("==== Exception Occurred for Job: %s ====" %
                       str(e))
            return FAILED

    @attr(tags=["advanced", "advancedns", "basic"], required_hardware="true")
    def test_Cancel_VM_Deployment(self):
        """
         1. Start Deploying a VM
         2. Cancel the VM deployment
         3. Verify Job status for vm deployment in async-job table is 3
         4. Verify  deploy VM  api returns exception

        """
        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create user api client of the account
        userapiclient = self.testClient.getUserApiClient(
            UserName=account.name,
            DomainName=account.domain
        )

        # Create VM
        asyncjobid = self.deployVM(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )
        time.sleep(20)
        AsyncJob.cancel(self.apiclient, asyncjobid.jobid)
        self.cleanup.append(account)
        self.verify_job_status(self.apiclient, asyncjobid.jobid)
        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % account.id
        )
        list_validation_result = validateList(qresultset)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])

        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug(
            "select VM instance id where type=User and account_id = '%s';" %
            account_id)

        qresultset = self.dbclient.execute(
            "select id from vm_instance where type =\"User\" and  account_id = '%s';" %
            account_id)
        list_validation_result = validateList(qresultset)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])

        qresult = qresultset[0]
        vm_id = qresult[0]
        self.debug("Query result: %s" % qresult)

        qresultset = self.dbclient.execute(
            "select id from async_job where instance_id = '%s';"
            % vm_id)
        list_validation_result = validateList(qresultset)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])

        qresult = qresultset[0]
        job_id = qresult[0]
        status = self.query_child_job_status((int(job_id) + 1))

        self.assertEqual(status, PASS, "child job status should be cancel")
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_Cancel_Volume_Snapshot_Creation(self):
        """Test to cancel volume snapshot that is being deployed

         1. Deploy  a VM
         2. Start deploying a Snapshot create command for the root disk of VM
         3. Cancel the snapshot creation
         4. Verify Job status for  snapshot creation in async-job table is 3
         5. Verify  the child jobs are cancelled

        """
        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup.append(account)

        # Create VM
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id

        )
        # Get the Root disk of VM
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )
        volume = volumes[0]

        # Deploy create snapshot command
        snapshotCreate = self.create_snapshot(self.apiclient, volume.id)
        time.sleep(20)

        AsyncJob.cancel(self.apiclient, snapshotCreate.jobid)
        self.verify_job_status(self.apiclient, snapshotCreate.jobid)

        # Get the ID from snapshots table from uuid
        self.debug("Get the ID from snapshots table from uuid = '%s';"
                   % snapshotCreate.id)

        qresultset = self.dbclient.execute(
            "select id from snapshots where uuid = '%s';"
            % snapshotCreate.id
        )
        list_validation_result = validateList(qresultset)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])

        qresult = qresultset[0]
        ss_id = qresult[0]

        # Get ID from async_job table using ss_id
        self.debug("Query result: %s" % qresult)

        qresultset = self.dbclient.execute(
            "select id from async_job where instance_id = '%s';"
            % ss_id)
        list_validation_result = validateList(qresultset)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])

        qresult = qresultset[0]
        job_id = qresult[0]
        status = self.query_child_job_status((int(job_id) + 1))
        self.assertEqual(status, PASS, "child job status should be cancel")
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "basic", "sa"],
        required_hardware="true")
    def test_Long_Running_Jobs(self):
        """
         1. create 5 different account
         2. Create VM deployment in each account
         3. Call long running async job api with root admin privilege
         4. Verify  api returns list of all the long running jobs from all the above accounts
         5. Call long running async job api with root admin privilege for single account
         6. Verify Jobs relatec to only that acount is listed
         7. Call long running async job api with normal user privilege
         8. Verify job fails as users are not allowed to call the api

        """

        start_time = time.time()
        for i in range(0, 5):
            account = Account.create(
                self.apiclient,
                self.testdata["account"],
                domainid=self.domain.id
            )
            self.cleanup.append(account)
            # Create user api client of the account
            userapiclient = self.testClient.getUserApiClient(
                UserName=account.name,
                DomainName=account.domain
            )
            asyncjobid = self.deployVM(
                self.apiclient,
                self.testdata["small"],
                templateid=self.template.id,
                accountid=account.name,
                domainid=account.domainid,
                serviceofferingid=self.service_offering.id,
                zoneid=self.zone.id
            )

        endtime = time.time() - start_time
        time.sleep(60)
        # call long running async job with admin privelage
        list_long_job = AsyncJob.list_long_running(
            self.apiclient, int(endtime))
        list_validation_result = validateList(list_long_job)
        # verify list is returned
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])
        # verify all the 5 vm deployed in above loop is returned
        self.assertEqual(
            len(list_long_job),
            5,
            " Not listing all the deploy VM jobs")

        # executing AP Ifrom user crediantial. Will result in exception
        try:
            list_long_job = AsyncJob.list_long_running(
                userapiclient, int(endtime))

        except Exception as e:
            self.debug("exception raised %s " % e)
            self.assertRaises("Exception Raised : %s" % e)

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "basic",
            "oen"],
        required_hardware="true")
    def test_QueuedUp_Async_Jobs(self):
        """
         1. Create an account and deploy a VM in that account
         2. Fire a volume snapshot command for the root volume of VM.
         3. While the SS is coming up,simultaneously fire  4 SS
            commands on the same volume
         4. Call list queued up async job api with root admin privilege
         5. Verify  api returns list of all the queued up jobs
         6. Call list queuedup async job api with normal user privilege
         7. Verify job fails as users are not allowed to call the api

        """
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create user api client of the account
        userapiclient = self.testClient.getUserApiClient(
            UserName=account.name,
            DomainName=account.domain
        )
        self.virtual_machine= VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.virtual_machine)
        self.cleanup.append(account)

        # Get the Root disk of VM
        volumes = list_volumes(
            self.apiclient,
            type='ROOT',
            virtualmachineid=self.virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check list response returns a valid list"
        )
        volume1 = volumes[0]

        # create a snapshot command for volume1
        snapshotcreate = self.create_snapshot(self.apiclient, volume1.id)
        asyncid_ss = snapshotcreate.jobid

        time.sleep(5)

        async_id = []
        ss_uuid = []
        for j in [1, 2]:
            successive_ss = self.create_snapshot(self.apiclient, volume1.id)
            async_id.append(successive_ss.jobid)
            ss_uuid.append(successive_ss.id)

        time.sleep(5)

        list_queuedup_job = AsyncJob.list_queued_jobs(
            self.apiclient, snapshotcreate.jobid)
        list_validation_result = validateList(list_queuedup_job)

        # verify list is returned
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])

        # verify all the 3 queued up jobs in above loop is returned
        self.assertEqual(
            len(list_queuedup_job),
            3,
            " Not listing all the queued up async jobs")

        # executing API from user crediantials. Will result in exception

        with self.assertRaises(Exception):
            AsyncJob.list_queued_jobs(
                userapiclient, snapshotcreate.jobid)
            self.debug("exception occured: API successful from user account")

        # Here, query the status of the snapshots and
        # delete them once they come into 'BackedUp' state
        self.query_async_job(self.apiclient, asyncid_ss)
        for k in [0,len(async_id)-1]:

            self.query_async_job(self.apiclient, async_id[k])
            self.delete_snapshot(self.apiclient, ss_uuid[k])
            self.debug("Deleting snapshot after they come to "
                       "BackdUp state")

        self.delete_snapshot(self.apiclient, snapshotcreate.id)
        return
