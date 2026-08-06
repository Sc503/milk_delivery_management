package in.esmartsolution.milkflow.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import in.esmartsolution.milkflow.models.Staff;

import java.util.List;

@Dao
public interface StaffDao {

    @Insert
    long insert(Staff staff);

    //  ADD THIS METHOD - Insert multiple staff members
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Staff> staffList);

    @Update
    void update(Staff staff);

    @Delete
    void delete(Staff staff);

    @Query("SELECT * FROM staff ORDER BY name ASC")
    LiveData<List<Staff>> getAllStaff();


    @Query("SELECT * FROM staff WHERE id=:id LIMIT 1")
    Staff getStaffById(long id);

    //  ADD THIS METHOD - Delete all staff (optional, for clean sync)
    @Query("DELETE FROM staff")
    void deleteAll();
}