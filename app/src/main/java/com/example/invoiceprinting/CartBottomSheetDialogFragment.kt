package com.example.invoiceprinting

import android.app.Dialog
import android.view.View
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class CartBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var dialogOffsetText: TextView? = null
    private var dialogStateText: TextView? = null
    private val bottomSheetBehaviorCallback: BottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        val contentView = View.inflate(context, R.layout.cart_bottom_sheet_fragment, null)
        dialog.setContentView(contentView)
        val layoutParams = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.setBottomSheetCallback(bottomSheetBehaviorCallback)
        }
        dialogOffsetText = contentView.findViewById<View>(R.id.dialogOffsetText) as TextView
        dialogStateText = contentView.findViewById<View>(R.id.dialogStateText) as TextView
    }




}