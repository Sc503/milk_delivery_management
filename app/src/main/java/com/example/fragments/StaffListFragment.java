package com.example.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.R;
import com.example.adapters.StaffAdapter;
import com.example.databinding.FragmentStaffListBinding;
import com.example.dialogs.EditStaffDialog;

import com.example.dialogs.StaffDetailsDialog;
import com.example.models.Staff;
import com.example.utils.StaffPdfExporter;
import com.example.viewmodel.MilkViewModel;

import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.widget.SearchView;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.dialogs.AddStaffDialog;

import java.io.File;

public class StaffListFragment extends Fragment {

    private FragmentStaffListBinding binding;
    private MilkViewModel viewModel;
    private StaffAdapter adapter;

    public StaffListFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentStaffListBinding.inflate(
                inflater,
                container,
                false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);

        viewModel = new ViewModelProvider(requireActivity()).get(MilkViewModel.class);

        adapter = new StaffAdapter(new StaffAdapter.Listener() {

            @Override
            public void onEdit(Staff staff) {

                EditStaffDialog dialog =
                        new EditStaffDialog(
                                staff,
                                updatedStaff -> {

                                    viewModel.updateStaff(updatedStaff);

                                    Toast.makeText(
                                            requireContext(),
                                            "Updated Successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });

                dialog.show(
                        getParentFragmentManager(),
                        "EDIT_STAFF");
            }

            @Override
            public void onDelete(Staff staff) {

                showDeleteDialog(staff);
            }

            @Override
            public void onCall(Staff staff) {

                Intent intent = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + staff.getMobile1()));

                startActivity(intent);
            }

            @Override
            public void onDetails(Staff staff) {

                new StaffDetailsDialog(staff)
                        .show(
                                getParentFragmentManager(),
                                "STAFF_DETAILS");

                Toast.makeText(
                        requireContext(),
                        staff.getName(),
                        Toast.LENGTH_SHORT
                ).show();
            }


        });


        binding.recyclerStaff.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        binding.recyclerStaff.setAdapter(adapter);

        new ItemTouchHelper(

                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT) {

                    @Override
                    public boolean onMove(
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target) {

                        return false;
                    }

                    @Override
                    public void onSwiped(
                            RecyclerView.ViewHolder viewHolder,
                            int direction) {

                        int pos = viewHolder.getAdapterPosition();

                        Staff staff = adapter.getItem(pos);

                        viewModel.deleteStaff(staff);

                    }
                }

        ).attachToRecyclerView(
                binding.recyclerStaff);

        binding.searchView.setOnQueryTextListener(

                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {

                        adapter.filter(newText);

                        return true;
                    }
                });

        observeStaff();

        binding.btnSharePdf.setOnClickListener(v -> {
            try {
                File file = StaffPdfExporter.export(requireContext(), adapter.getCurrentList());
                shareFile(file);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void observeStaff() {

        viewModel.getAllStaff()
                .observe(getViewLifecycleOwner(), staffList -> {

                    adapter.setData(staffList);
                    adapter.notifyDataSetChanged(); // 🔥 ADD THIS
                });
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share PDF"));
    }

    private void showDeleteDialog(Staff staff) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Staff")
                .setMessage(
                        "Delete " +
                                staff.getName() +
                                " ?")
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            viewModel.deleteStaff(staff);

                            Toast.makeText(
                                    requireContext(),
                                    "Deleted",
                                    Toast.LENGTH_SHORT
                            ).show();

                        })
                .setNegativeButton(
                        "Cancel",
                        null)
                .show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu,
                                    @NonNull MenuInflater inflater) {

        inflater.inflate(R.menu.staff_toolbar_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_add_staff) {

            new AddStaffDialog(

                    staff -> viewModel.insertStaff(staff)

            ).show(

                    getParentFragmentManager(),

                    "ADD_STAFF"

            );

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;
    }
}