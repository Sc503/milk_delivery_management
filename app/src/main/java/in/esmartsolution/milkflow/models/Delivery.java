package in.esmartsolution.milkflow.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "deliveries",
        foreignKeys = @ForeignKey(
                entity = Customer.class,
                parentColumns = "id",
                childColumns = "customerId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(
                        value = {
                                "customerId",
                                "deliveryDate"
                        },
                        unique = true
                )
        }
)
public class Delivery {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long customerId;
    private String deliveryDate; // YYYY-MM-DD
    private String deliveredTime; // e.g. "07:45 AM"
    private String status; // "Pending" or "Delivered"
    private long staffId;//  Staff who delivered

    private String staffName;

    public Delivery() {
    }

    public Delivery(long customerId, String deliveryDate, String deliveredTime, String status) {
        this.customerId = customerId;
        this.deliveryDate = deliveryDate;
        this.deliveredTime = deliveredTime;
        this.status = status;
        this.staffId = 0; // Default: no staff assigned
    }

    //  Constructor with staffId
    public Delivery(long customerId, String deliveryDate, String deliveredTime, String status, long staffId,String staffName) {
        this.customerId = customerId;
        this.deliveryDate = deliveryDate;
        this.deliveredTime = deliveredTime;
        this.status = status;
        this.staffId = staffId;
        this.staffName = staffName;

    }

    // ── Getters and Setters ──────────────────────────────────────
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getDeliveredTime() { return deliveredTime; }
    public void setDeliveredTime(String deliveredTime) { this.deliveredTime = deliveredTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    //  Getter and Setter for staffId
    public long getStaffId() { return staffId; }
    public void setStaffId(long staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
}
