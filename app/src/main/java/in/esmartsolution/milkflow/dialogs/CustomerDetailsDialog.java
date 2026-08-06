package in.esmartsolution.milkflow.dialogs;

import static android.content.Context.MODE_PRIVATE;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import in.esmartsolution.milkflow.databinding.DialogCustomerDetailsBinding;
import in.esmartsolution.milkflow.models.Customer;
import in.esmartsolution.milkflow.utils.PermissionManager;

public class CustomerDetailsDialog extends DialogFragment {

    private final Customer customer;
    private final String staffDisplay;
    private final DialogCallback callback;
    private DialogCustomerDetailsBinding binding;

    public interface DialogCallback {
        void onDeliver(Customer customer);
        void onEdit(Customer customer);
        void onCancel();
    }

    public CustomerDetailsDialog(Customer customer, String staffDisplay, DialogCallback callback) {
        this.customer = customer;
        this.staffDisplay = staffDisplay;
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogCustomerDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.dialogTxtName.setText(customer.getName());
        binding.dialogTxtMobile.setText(customer.getMobile());
        binding.dialogTxtAddress.setText(customer.getAddress());

        //  SET STAFF DISPLAY
        if (binding.dialogTxtStaffName != null) {
            String displayText = (staffDisplay != null && !staffDisplay.isEmpty() && !staffDisplay.equals("Not assigned"))
                    ?  staffDisplay
                    : "Not assigned";
            binding.dialogTxtStaffName.setText(displayText);
            binding.dialogTxtStaffName.setVisibility(View.VISIBLE);
        }

        String currentUserType =
                requireActivity()
                        .getSharedPreferences("UserSession", MODE_PRIVATE)
                        .getString("userType", "");

        if (!PermissionManager.canDeliver(currentUserType)) {
            binding.dialogBtnDeliver.setVisibility(View.GONE);
        }

        binding.dialogBtnCall.setOnClickListener(v -> {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customer.getMobile()));
            startActivity(dialIntent);
        });

        binding.dialogBtnNavigate.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + customer.getLatitude() + "," + customer.getLongitude());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });

        binding.dialogBtnDeliver.setOnClickListener(v -> {
            if (callback != null) callback.onDeliver(customer);
            dismiss();
        });

        binding.dialogBtnEdit.setOnClickListener(v -> {
            if (callback != null) {
                dismiss();
                callback.onEdit(customer);
            }
        });

        binding.dialogBtnCancel.setOnClickListener(v -> {
            if (callback != null) callback.onCancel();
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}