package in.esmartsolution.milkflow.backup;

import in.esmartsolution.milkflow.models.Customer;
import in.esmartsolution.milkflow.models.Delivery;

import java.util.List;

public class BackupData {

    private List<Customer> customers;
    private List<Delivery> deliveries;

    public BackupData() {
    }

    public BackupData(
            List<Customer> customers,
            List<Delivery> deliveries
    ) {
        this.customers = customers;
        this.deliveries = deliveries;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    public List<Delivery> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<Delivery> deliveries) {
        this.deliveries = deliveries;
    }
}