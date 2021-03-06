package org.cloudbus.cloudsim.examples;

/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */
import org.cloudsimplus.util.tablebuilder.CloudletsTableBuilderHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSimple;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostSimple;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.resources.FileStorage;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSimple;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Bandwidth;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Ram;
import org.cloudbus.cloudsim.schedulers.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.VmSchedulerSpaceShared;

/**
 * An example showing how to delay the submission of cloudlets.
 * Even there is enough resources to run all cloudlets simultaneously,
 * the example delays the creation of each cloudlet inside a VM,
 * simulating the dynamic arrival of cloudlets.
 * For each instantiated cloudlet will be defined a different
 * submission delay.
 * Even there is enough resources to run all cloudlets simultaneously,
 * it is used a CloudletSchedulerTimeShared, analyzing the output
 * you can see that each cloudlet starts in a different time,
 * simulating different arrivals.
 *
 * @author Manoel Campos da Silva Filho
 */
public class DynamicCloudletsArrival2 {
    /**
     * Number of Processor Elements (CPU Cores) of each Host.
     */
    private static final int HOST_PES_NUMBER = 4; 

    /**
     * Number of Processor Elements (CPU Cores) of each VM and cloudlet.
     */
    private static final int VM_PES_NUMBER = HOST_PES_NUMBER;    
    
    /**
     * Number of Cloudlets to create simultaneously.
     * Other cloudlets will be enqueued.
     */
    private static final int NUMBER_OF_CLOUDLETS = VM_PES_NUMBER; 
    
    /**
     * The Virtual Machine Monitor (VMM) used by hosts to manage VMs.
     */
    private static final String VMM = "Xen"; 
    
    private final List<Host> hostList;
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;
    private final DatacenterBroker broker;
    private final Datacenter datacenter;
    
    /**
     * Starts the example execution, calling the class constructor\
     * to build and run the simulation.
     * 
     * @param args command line parameters
     */
    public static void main(String[] args) {
        Log.printFormattedLine("Starting %s ...", DynamicCloudletsArrival2.class.getSimpleName());
        try {
            new DynamicCloudletsArrival2();
            Log.printFormattedLine("%s finished!", DynamicCloudletsArrival2.class.getSimpleName());        
        } catch (Exception e) {
            Log.printFormattedLine("Simulation finished due to unexpected error: %s", e);
        }
    }

    /**
     * Default constructor that builds and starts the simulation.
     */
    public DynamicCloudletsArrival2() {
        int numberOfUsers = 1; // number of cloud users/customers (brokers)
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(numberOfUsers, calendar);
        
        this.hostList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
        this.datacenter = createDatacenter("Datacenter0");
        this.broker = new DatacenterBrokerSimple("Broker0");

        createAndSubmitVmAndCloudlets();
        
        runSimulationAndPrintResults();
    }

    private void runSimulationAndPrintResults() {
        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        List<Cloudlet> cloudlets = broker.getCloudletsFinishedList();
        new CloudletsTableBuilderHelper(cloudlets).build();
    }

    /**
     * Creates cloudlets and submit them to the broker, applying
     * a different submission delay for each one (simulating the dynamic cloudlet arrival).
     * 
     * @param vm Vm to run the cloudlets to be created
     * 
     * @see #createCloudlet(int, org.cloudbus.cloudsim.Vm) 
     */
    private void createAndSubmitCloudlets(Vm vm) {
        int cloudletId = cloudletList.size();
        double submissionDelay = 0;
        List<Cloudlet> list = new ArrayList<>(NUMBER_OF_CLOUDLETS);
        for(int i = 0; i < NUMBER_OF_CLOUDLETS; i++){
            Cloudlet cloudlet = createCloudlet(cloudletId++, vm, broker);
            cloudlet.setSubmissionDelay(submissionDelay);
            submissionDelay += 10;
            list.add(cloudlet);
        }

        broker.submitCloudletList(list);
        cloudletList.addAll(list);
    }

    /**
     * Creates one Vm and a group of cloudlets to run inside it, 
     * and submit the Vm and its cloudlets to the broker.
     * 
     * @see #createVm(int, org.cloudbus.cloudsim.brokers.DatacenterBroker) 
     */
    private void createAndSubmitVmAndCloudlets() {
        List<Vm> list = new ArrayList<>();
        Vm vm = createVm(this.vmList.size(), broker);
        list.add(vm);
        
        broker.submitVmList(list);
        this.vmList.addAll(list);
        
        createAndSubmitCloudlets(vm);  
    }

    /**
     * Creates a VM with pre-defined configuration.
     * 
     * @param id the VM id
     * @param broker the broker that will be submit the VM
     * @return the created VM
     * 
     * @see #createVmListener() 
     */
    private Vm createVm(int id, DatacenterBroker broker) {
        int mips = 1000;
        long size = 10000; // image size (MB)
        int ram = 512; // vm memory (MB)
        long bw = 1000;
        Vm vm = new VmSimple(
                id, broker.getId(), mips, VM_PES_NUMBER, ram, bw, size,
                VMM, new CloudletSchedulerTimeShared());
        return vm;
    }

    /**
     * Creates a cloudlet with pre-defined configuration.
     * 
     * @param id Cloudlet id
     * @param vm vm to run the cloudlet
     * @param broker the broker that will submit the cloudlets
     * @return the created cloudlet
     */
    private Cloudlet createCloudlet(int id, Vm vm, DatacenterBroker broker) {
        long fileSize = 300;
        long outputSize = 300;
        long length = 10000; //in number of Million Instructions (MI)
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Cloudlet cloudlet
                = new CloudletSimple(id, length, pesNumber, fileSize,
                        outputSize, utilizationModel, utilizationModel,
                        utilizationModel);
        cloudlet.setUserId(broker.getId());
        cloudlet.setVmId(vm.getId());
        
        return cloudlet;
    }

    /**
     * Creates a datacenter with pre-defined configuration.
     *
     * @param name the datacenter name
     * @return the created datacenter
     */
    private Datacenter createDatacenter(String name) {
        Host host = createHost(0);
        hostList.add(host); 

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this datacenter
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<FileStorage> storageList = new LinkedList<>(); // we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristicsSimple(
                arch, os, VMM, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        return new DatacenterSimple(name, characteristics, 
                new VmAllocationPolicySimple(hostList), storageList, 0);
    }

    /**
     * Creates a host with pre-defined configuration.
     * 
     * @param id The Host id
     * @return the created host
     */
    private Host createHost(int id) {
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;
        for(int i = 0; i < HOST_PES_NUMBER; i++){
            peList.add(new PeSimple(i, new PeProvisionerSimple(mips)));
        }
        int ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage
        long bw = 10000;
        
        return new HostSimple(id,
                new ResourceProvisionerSimple<>(new Ram(ram)),
                new ResourceProvisionerSimple<>(new Bandwidth(bw)),
                storage, peList, new VmSchedulerSpaceShared(peList));
    }
}
