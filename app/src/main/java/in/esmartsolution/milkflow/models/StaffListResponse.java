package in.esmartsolution.milkflow.models;

import java.util.List;

public class StaffListResponse {
    private boolean Status;
    private String Message;
    private List<Staff> Data;

    public boolean isStatus() { return Status; }
    public void setStatus(boolean Status) { this.Status = Status; }

    public String getMessage() { return Message; }
    public void setMessage(String Message) { this.Message = Message; }

    public List<Staff> getData() { return Data; }
    public void setData(List<Staff> Data) { this.Data = Data; }
}