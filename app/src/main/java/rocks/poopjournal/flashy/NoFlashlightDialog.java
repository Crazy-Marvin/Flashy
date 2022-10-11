package rocks.poopjournal.flashy;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import rocks.poopjournal.flashy.databinding.DialogNoFlashlightBinding;

public class NoFlashlightDialog extends BottomSheetDialogFragment {
    public static final String NO_FLASH_DIALOG_DISMISSED = "no_flash_dialog_dismissed";
    private DialogNoFlashlightBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogNoFlashlightBinding.inflate(requireActivity().getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(binding.getRoot());
        binding.containerUseScreen.setOnClickListener(v -> dismiss());
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        getParentFragmentManager().setFragmentResult(NO_FLASH_DIALOG_DISMISSED, new Bundle());
        binding = null;
    }
}
