package in.esmartsolution.milkflow.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import in.esmartsolution.milkflow.models.Attendance;

import java.util.List;

@Dao
public interface AttendanceDao {

    @Insert
    void insert(Attendance attendance);

    @Query("SELECT * FROM attendance WHERE staffId=:staffId")
    List<Attendance> getAttendance(long staffId);

    @Query("SELECT COUNT(*) FROM attendance WHERE staffId=:staffId AND present=1")
    int getPresentDays(long staffId);
}