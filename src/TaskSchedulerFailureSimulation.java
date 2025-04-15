import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


import java.util.*;

public class TaskSchedulerFailureSimulation {

    public static void main(String[] args) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            Datacenter datacenter = createDatacenter("Datacenter_1");
            DatacenterBroker broker = new DatacenterBroker("Broker_1");

            int brokerId = broker.getId();

            List<Vm> vmList = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                vmList.add(new Vm(i, brokerId, 1000, 1, 1024, 10000, 1000,
                        "Xen", new CloudletSchedulerTimeShared()));
            }
            broker.submitVmList(vmList);

            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilizationModel = new UtilizationModelFull();

            for (int i = 0; i < 4; i++) {
                Cloudlet cloudlet = new Cloudlet(i, 40000, 1, 512, 300,
                        utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);
            CloudSim.startSimulation();

            List<Cloudlet> finishedList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            System.out.println("\n===== Cloudlet Execution Results =====");
            for (Cloudlet cloudlet : finishedList) {
                String status = cloudlet.getStatus() == Cloudlet.SUCCESS ? "SUCCESS" : "FAILED";
                System.out.printf("Cloudlet ID: %d | VM ID: %d | Status: %s\n",
                        cloudlet.getCloudletId(), cloudlet.getVmId(), status);
            }
            Log.printLine("Test done successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000)));

        int ram = 2048;
        long storage = 1000000;
        int bw = 10000;

        hostList.add(new Host(0, new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw), storage, peList,
                new VmSchedulerTimeShared(peList)));

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList,
                10.0, 3.0, 0.05, 0.1, 0.1);

        return new Datacenter(name, characteristics,
                new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
    }
}
