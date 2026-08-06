package in.esmartsolution.milkflow.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "staff")
public class Staff {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private int account_id;
    private String name;
    private String usertype;
    private String mobile;
    private String mobile2;
    private String address;
    private String documentPath;
    private String documentType;
    private int isactive;

    private String password;

    // ── Constructors ──────────────────────────────────────────────
    public Staff() {}

    public Staff(String name, String mobile) {
        this.name = name;
        this.mobile = mobile;
        this.usertype = "staff";
        this.isactive = 1;
    }

    public Staff(String name, String mobile, String mobile2, String address, String documentPath, String documentType) {
        this.name = name;
        this.mobile = mobile;
        this.mobile2 = mobile2;
        this.address = address;
        this.documentPath = documentPath;
        this.documentType = documentType;
        this.usertype = "staff";
        this.isactive = 1;
    }

    // ── Getters and Setters ──────────────────────────────────────
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getAccount_id() { return account_id; }
    public void setAccount_id(int account_id) { this.account_id = account_id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsertype() { return usertype; }
    public void setUsertype(String usertype) { this.usertype = usertype; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getMobile2() { return mobile2; }
    public void setMobile2(String mobile2) { this.mobile2 = mobile2; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public int getIsactive() { return isactive; }
    public void setIsactive(int isactive) { this.isactive = isactive; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}