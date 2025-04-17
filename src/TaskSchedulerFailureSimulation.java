import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;

public class TaskSchedulerFailureSimulation {

    private static final double FAILURE_PROBABILITY = 0.3;  // 30% failure rate
    private static final Set<Integer> failedCloudletIds = new HashSet<>();
    private static final Map<Integer, Double> cloudletDeadlines = new HashMap<>();
    private static final double PENALTY_PER_VIOLATION = 50.0;

    public static void main(String[] args) {
        try {
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;

            CloudSim.init(numUsers, calendar, traceFlag);

            Datacenter datacenter = createDatacenter("Datacenter_1");
            DatacenterBroker broker = new DatacenterBroker("Broker_1");

            int brokerId = broker.getId();

            // Create 2 VMs
            List<Vm> vmList = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                vmList.add(new Vm(i, brokerId, 1000, 1, 1024, 10000, 1000,
                        "Xen", new CloudletSchedulerTimeShared()));
            }
            broker.submitVmList(vmList);

            // Create 10 Cloudlets
            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilizationModel = new UtilizationModelFull();

            Random rand = new Random();
            for (int i = 0; i < 10; i++) {
                // Simulate random execution time between 1000 and 5000 units
                long length = 1000 + rand.nextInt(4000);  
                Cloudlet cloudlet = new Cloudlet(i, length, 1, 512, 300,
                        utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);

                // Simulate deadline (random between 20–50 units)
                double deadline = 20 + rand.nextDouble() * 30;
                cloudletDeadlines.put(i, deadline);

                // Simulate random failure
                if (rand.nextDouble() < FAILURE_PROBABILITY) {
                    failedCloudletIds.add(i);
                    // Set a longer execution time for failed cloudlets
                    cloudlet.setCloudletLength(rand.nextInt(5000));  // Random execution time for failed cloudlets
                }

                cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);
            CloudSim.startSimulation();

            List<Cloudlet> finishedList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Output
            System.out.println("\n===== Cloudlet Execution Results =====");
            int success = 0;
            int failed = 0;
            int violations = 0;
            double totalPenalty = 0.0;

            for (Cloudlet cloudlet : finishedList) {
                String status;
                int id = cloudlet.getCloudletId();
                double actualTime = cloudlet.getActualCPUTime();
                double deadline = cloudletDeadlines.get(id);
                boolean slaViolated = false;

                if (failedCloudletIds.contains(id)) {
                    status = "FAILED";
                    failed++;
                    // Failed cloudlets should not be compared for SLA violations
                } else {
                    status = (cloudlet.getStatus() == Cloudlet.SUCCESS) ? "SUCCESS" : "FAILED";
                    if (status.equals("SUCCESS")) success++; else failed++;

                    // Check SLA violation
                    if (actualTime > deadline) {
                        slaViolated = true;
                    }
                }

                if (slaViolated) {
                    violations++;
                    totalPenalty += PENALTY_PER_VIOLATION;
                }

                System.out.printf("Cloudlet ID: %d | VM ID: %d | Status: %s | ExecTime: %.2f | Deadline: %.2f | SLA Violation: %s\n",
                        id, cloudlet.getVmId(), status, actualTime, deadline, slaViolated ? "YES" : "NO");
            }

            System.out.println("=======================================");
            System.out.printf("Total Cloudlets: %d | Success: %d | Failed: %d\n", finishedList.size(), success, failed);
            System.out.printf("SLA Violations: %d | Total Penalty: $%.2f\n", violations, totalPenalty);
            System.out.println("=======================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(1000)));  // Simulate a processing unit with 1000 MIPS

        int ram = 2048;
        long storage = 1000000;
        int bw = 20000;  // Increased bandwidth to allow both VMs

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
