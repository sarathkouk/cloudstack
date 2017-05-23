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
import time

from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import (
    PASS,
    FAILED,
    JOB_SUCCEEDED,
    JOB_CANCELLED,
    JOB_FAILED)
from marvin.lib.base import (
    ServiceOffering,
    Account,
    AsyncJob,
    Template,
    DiskOffering,
    VirtualMachine,
    Network,
    NetworkOffering)
from marvin.lib.common import (get_domain,
                               get_zone,
                               list_snapshots,
                               list_volumes,
                               get_template,
                               validateList,
                               get_builtin_template_info
                               )
from marvin.lib.utils import (cleanup_resources, random_gen)
from nose.plugins.attrib import attr


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
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        builtin_info = get_builtin_template_info(cls.apiclient, cls.zone.id)
        if str(builtin_info[1]).lower() == "vmware":
            cls.testdata["privatetemplate"][
                "url"] = "http://s3.download.accelerite.com/templates/builtin/centos65-x86_64-vmware.ova"
        elif str(builtin_info[1]).lower() == "xenserver":
            cls.testdata["privatetemplate"][
                "url"] = "http://s3.download.accelerite.com/templates/builtin/centos65-x86_64-xen.vhd.bz2"
        elif str(builtin_info[1]).lower() == "kvm":
            cls.testdata["privatetemplate"][
                "url"] = ""
        elif str(builtin_info[1]).lower() == "hyperv":
            cls.testdata["privatetemplate"][
                "url"] = "http://s3.download.accelerite.com/templates/builtin/centos65-x86_64-hyperv.vhd.bz2"
        cls.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        cls.testdata["privatetemplate"]["format"] = builtin_info[2]
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
        # Create an account
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

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

    def add_nic(self, apiclient, vmid, networkId, ipaddress=None):
        """Add a NIC to a VM"""
        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.isAsync = "false"
        cmd.virtualmachineid = vmid
        cmd.networkid = networkId

        if ipaddress:
            cmd.ipaddress = ipaddress

        return apiclient.addNicToVirtualMachine(cmd)

    def stop(self, apiclient, vmid, forced=None):
        """Stop the instance"""
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = vmid
        cmd.isAsync = "false"
        if forced:
            cmd.forced = forced
        return apiclient.stopVirtualMachine(cmd)

    def reboot(self, apiclient, vmid):
        """Reboot the instance"""
        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = vmid
        cmd.isAsync = "false"
        return apiclient.rebootVirtualMachine(cmd)

    def start(self, apiclient, vmid):
        """Start the instance"""
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = vmid
        cmd.isAsync = "false"
        return apiclient.startVirtualMachine(cmd)

    def restore(self, apiclient, vmid, templateid):
        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid = vmid
        cmd.templateid = templateid
        cmd.isAsync = "false"
        return apiclient.restoreVirtualMachine(cmd)

    def destroy(self, apiclient, vmid, expunge=True, **kwargs):
        """Destroy an Instance"""
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = vmid
        cmd.isAsync = "false"
        cmd.expunge = expunge
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.destroyVirtualMachine(cmd)

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
        """Verify the status for Async Job"""
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
    def test_Cancel_Add_NIC_to_VM(self):
        """
         1. Deploy a VM
         2. Create a new network
         3. Add the newly created network to VM  deployed in step 1
         4. Cancel addition of network
         5. Verify Job status for nic addition in async-job table is 3
         4. Verify  add nic  api returns exception

        """
        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.cleanup.append(virtual_machine)
        isolated_network_offering = NetworkOffering.create(
            self.apiclient, self.testdata["isolated_network_offering"])
        isolated_network_offering.update(self.apiclient, state='Enabled')
        isolated_network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            accountid=account.name,
            domainid=self.domain.id,
            networkofferingid=isolated_network_offering.id)
        self.debug(
            "Adding %s Network: %s to virtual machine %s" %
            (isolated_network.type, isolated_network.id, virtual_machine.id))

        asyncjobid = self.add_nic(
            self.apiclient,
            virtual_machine.id,
            isolated_network.id)
        time.sleep(20)
        AsyncJob.cancel(self.apiclient, asyncjobid.jobid)

        isolated_network_offering.update(self.apiclient, state="Disabled")
        self.cleanup.append(isolated_network)
        self.cleanup.append(isolated_network_offering)
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
            "select VM instance uuid where type=User and account_id = '%s';" %
            account_id)

        qresultset = self.dbclient.execute(
            "select uuid from vm_instance where type =\"User\" and  account_id = '%s';" %
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

        self.debug(
            "select id from async_job where instance_type = \"None\" and job_cmd_info  LIKE \'%%%s%%\' ;" %
            vm_id)

        qresultset = self.dbclient.execute(
            "select id from async_job where instance_type = \"None\" and job_cmd_info  LIKE \'%%%s%%\' ;" %
            vm_id)
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

    @attr(tags=["advanced", "advancedns", "basic"], required_hardware="true")
    def test_Cancel_Stop_VM(self):
        """
         1. Deploy a VM
         2. When VM is running try to stop the VM
         4. Cancel stop VM
         5. Verify Job status for Stop VM in async-job table is 3
         4. Verify  Stop VM  api returns exception

        """
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.cleanup.append(virtual_machine)
        self.cleanup.append(account)
        asyncjobid = self.stop(self.apiclient, virtual_machine.id)
        time.sleep(2)

        AsyncJob.cancel(self.apiclient, asyncjobid.jobid)

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
            "select id from async_job where instance_id = '%s' and job_cmd  LIKE \'%%StopVMCmd%%\';" %
            vm_id)
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
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        list_validation_result = validateList(vms)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])
        self.assertEqual(vms[0].state, "Running", " VM should be running")

        return

    @attr(tags=["advanced", "advancedns", "basic"], required_hardware="true")
    def test_Cancel_Reboot_VM(self):
        """
         1. Deploy a VM
         2. When VM is running try to reboot the VM
         4. Cancel reboot VM
         5. Verify Job status for reboot VM in async-job table is 3
         4. Verify  reboot VM  api returns exception"""

        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.cleanup.append(virtual_machine)
        self.cleanup.append(account)
        asyncjobid = self.reboot(self.apiclient, virtual_machine.id)
        time.sleep(2)

        AsyncJob.cancel(self.apiclient, asyncjobid.jobid)
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
            "select id from async_job where instance_id = '%s' and job_cmd  LIKE \'%%RebootVMCmd%%\';" %
            vm_id)
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
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        list_validation_result = validateList(vms)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])
        self.assertEqual(vms[0].state, "Running", " VM should be running")

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "basic",
            "sa"],
        required_hardware="true")
    def test_Cancel_Start_VM(self):
        """
         1. Deploy a VM
         2. Stop the VM . Try to start the VM
         4. Cancel start VM
         5. Verify Job status for Start VM in async-job table is 3
         4. Verify  Start VM  api returns exception
         5. Verify VM is in stop state
         """
        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.cleanup.append(virtual_machine)
        self.cleanup.append(account)
        virtual_machine.stop(self.apiclient)
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        list_validation_result = validateList(vms)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])
        self.assertEqual(vms[0].state, "Stopped", " VM should be Stopped")

        asyncjobid = self.start(self.apiclient, virtual_machine.id)

        AsyncJob.cancel(self.apiclient, asyncjobid.jobid)

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
            "select id from async_job where instance_id = '%s' and job_cmd  LIKE \'%%StartVMCmd%%\';" %
            vm_id)
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
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        list_validation_result = validateList(vms)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])
        self.assertEqual(vms[0].state, "Stopped", " VM should be Stopped")

        return

    @attr(
        tags=["advanced", "advancedns", "basic"], required_hardware="true")
    def test_Cancel_Reset_VM(self):
        """Negative test to verify Admin can not cancel restore VM
         1. Deploy a VM
         2. When VM is running try to reset the VM  with new template
         4. Cancel reset VM
         5. Verify Cancel of reste VM  fails
         4. Verify  Vm is restored with new template and running
        """

        # Create an account
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment")

        self.cleanup.append(virtual_machine)

        template_created = Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            self.zone.id,
            hypervisor=self.hypervisor,
            account=account.name,
            domainid=account.domainid

        )

        self.assertIsNotNone(
            template_created,
            "Template creation failed"
        )
        template_created.download(self.apiclient)

        # Wait for template status to be changed across
        time.sleep(self.testdata["sleep"])

        list_template_response = Template.list(
            self.apiclient,
            templatefilter='all',
            id=template_created.id,
            account=account.name,
            domainid=account.domainid)

        list_validation_result = validateList(list_template_response)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])

        self.cleanup.append(template_created)
        self.cleanup.append(account)
        asyncjobid = self.restore(
            self.apiclient,
            virtual_machine.id,
            template_created.id)
        time.sleep(2)
        try:
            AsyncJob.cancel(self.apiclient, asyncjobid.jobid)
        except Exception as e:
            self.debug("Exception raised %s" % e)
            self.assertRaises("Exception raised: %s" % e)
            self.query_async_job(self.apiclient, asyncjobid.jobid)

            self.debug("Checking template id of VM")
            vms = VirtualMachine.list(
                self.apiclient,
                id=virtual_machine.id,
                listall=True
            )

            vm_list_validation_result = validateList(vms)

            self.assertEqual(
                vm_list_validation_result[0],
                PASS,
                "VM list validation failed due to %s" %
                vm_list_validation_result[2])

            vm_with_reset = vm_list_validation_result[1]

            self.assertEqual(
                vm_with_reset.templateid,
                template_created.id,
                "VM has not changed to expected templateid : %s after restore" %
                vm_with_reset.templateid)

        return

    @attr(tags=["advanced", "advancedns", "basic"], required_hardware="true")
    def test_Cancel_Destory_VM(self):
        """
         1. Deploy a VM
         2. When VM is running try to stop the VM
         4. Cancel stop VM
         5. Verify Job status for Stop VM in async-job table is 3
         4. Verify  Stop VM  api returns exception

        """
        account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        # Create VM
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=account.name,
            domainid=account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id)
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment"
        )

        self.cleanup.append(virtual_machine)
        self.cleanup.append(account)
        asyncjobid = self.destroy(self.apiclient, virtual_machine.id)
        time.sleep(2)

        AsyncJob.cancel(self.apiclient, asyncjobid.jobid)

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
            "select id from async_job where instance_id = '%s' and job_cmd  LIKE \'%%DestroyVMCmd%%\';" %
            vm_id)
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
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        list_validation_result = validateList(vms)
        self.assertEqual(
            list_validation_result[0],
            PASS,
            "list validation failed due to %s" %
            list_validation_result[2])
        self.assertEqual(vms[0].state, "Running", " VM should be running")

        return

class TestCancelJobVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCancelJobVolumes, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.testdata["isolated_network"]["zoneid"] = cls.zone.id
        builtin_info = get_builtin_template_info(cls.apiclient, cls.zone.id)
        if str(builtin_info[1]).lower() == "vmware":
            cls.testdata["privatetemplate"][
                "url"] = "http://s3.download.accelerite.com/templates/builtin/centos65-x86_64-vmware.ova"
        elif str(builtin_info[1]).lower() == "xenserver":
            cls.testdata["privatetemplate"][
                "url"] = "http://s3.download.accelerite.com/templates/builtin/centos65-x86_64-xen.vhd.bz2"
        elif str(builtin_info[1]).lower() == "kvm":
            cls.testdata["privatetemplate"][
                "url"] = "http://s3.download.accelerite.com/templates/builtin/centos65-x86_64-kvm.qcow2.bz2"
        elif str(builtin_info[1]).lower() == "hyperv":
            cls.testdata["privatetemplate"][
                "url"] = "http://s3.download.accelerite.com/templates/builtin/centos65-x86_64-hyperv.vhd.bz2"
        cls.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        cls.testdata["privatetemplate"]["format"] = builtin_info[2]
        cls.testdata["privatetemplate"]["ostype"] = cls.testdata["ostype"]
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"]
        )
        cls._cleanup = []

        # Create Service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"],
        )
        cls._cleanup.append(cls.service_offering)
        # Create an account
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            domainid=cls.domain.id
        )

        # Create VM
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.testdata["small"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            zoneid=cls.zone.id)

        cls._cleanup.append(cls.virtual_machine)
        cls._cleanup.append(cls.account)

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
                if job_status == 2:
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
        """Verify the status for Async Job"""
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

    def create_volume(cls, apiclient, services, zoneid=None, account=None,
                      domainid=None, diskofferingid=None, projectid=None, size=None):
        """Stop the instance"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join([services["diskname"], random_gen()])

        if diskofferingid:
            cmd.diskofferingid = diskofferingid
        elif "diskofferingid" in services:
            cmd.diskofferingid = services["diskofferingid"]

        if zoneid:
            cmd.zoneid = zoneid
        elif "zoneid" in services:
            cmd.zoneid = services["zoneid"]

        if account:
            cmd.account = account
        elif "account" in services:
            cmd.account = services["account"]

        if domainid:
            cmd.domainid = domainid
        elif "domainid" in services:
            cmd.domainid = services["domainid"]

        if projectid:
            cmd.projectid = projectid

        if size:
            cmd.size = size

        cmd.isAsync = "false"

        return apiclient.createVolume(cmd)


    def create_VM_SS(self, apiclient, vmid, snapshotmemory="false",
                     name=None, description=None, projectid=None):
        cmd = createVMSnapshot.createVMSnapshotCmd()
        cmd.virtualmachineid = vmid

        if snapshotmemory:
            cmd.snapshotmemory = snapshotmemory
        if name:
            cmd.name = name
        if description:
            cmd.description = description
        if projectid:
            cmd.projectid = projectid
        cmd.isAsync = "false"
        return apiclient.createVMSnapshot(cmd)

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



    @attr(tags=["advanced", "oen", "basic"], required_hardware="true")
    def test_cancel_VM_snapshot_create(self):
        """Cancels the Create VM snapshot command"""
        vm_ss = self.create_VM_SS(self.apiclient, self.virtual_machine.id)
        AsyncJob.cancel(self.apiclient, vm_ss.jobid)
        self.verify_job_status(self.apiclient, vm_ss.jobid)
        return

    @attr(tags=["advanced", "oen", "basic"], required_hardware="true")
    def test_cancel_volume_snapshot(self):

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
        time.sleep(5)

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

    @attr(tags=["advanced", "oen", "basic"], required_hardware="true")
    def test_cancel_queuedUp_snapshots(self):
        """Test to check other snapshots in queue are working properly
            after one snapshot creation  is cancelled"""

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

        AsyncJob.cancel(self.apiclient, snapshotcreate.jobid)
        self.verify_job_status(self.apiclient, snapshotcreate.jobid)

        self.query_async_job(self.apiclient, asyncid_ss)

        for k in [0, 1]:
            self.query_async_job(self.apiclient, async_id[k])

        list_ss = list_snapshots(
            self.apiclient,
            volumeid=volume1.id,
            listall=True
        )
        self.assertEqual(
            isinstance(list_ss, list),
            True,
            " Not a valid list"
        )

        self.assertEqual(
            len(list_ss),
            2,
            " Not listing all the snapshots")

        for k in [0, 1]:
            self.assertEqual(
                list_ss[k].state,
                "BackedUp",
                " Snapshot is not in BackedUp state")
        # delete them once they come into 'BackedUp' state

        for k in [0, len(async_id) - 1]:
            self.query_async_job(self.apiclient, async_id[k])
            self.delete_snapshot(self.apiclient, ss_uuid[k])
        return
