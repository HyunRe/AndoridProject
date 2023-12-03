package com.example.andoridproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import com.example.andoridproject.databinding.DialogFilterBinding

class FilterDialog : DialogFragment() {
    private val binding: DialogFilterBinding by lazy {
        DialogFilterBinding.inflate(layoutInflater)
    }
    private var onFilterListener: OnFilterListener? = null
    interface OnFilterListener {
        fun onFilter(appliedFilters: Filters)
    }

    fun setOnFilterListener(listener: OnFilterListener) {
        this.onFilterListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = binding.root

        // 다이얼로그 내부의 버튼이나 체크박스 등의 UI 요소들을 참조하여 이벤트 리스너 등을 설정
        binding.btnApply.setOnClickListener {
            // 필터가 적용되었을 때, 메인 Fragment에 알림
            val appliedFilters = gatherAppliedFilters()
            onFilterListener?.onFilter(appliedFilters)
            dismiss()
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        // 다이얼로그 크기 조정
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.setLayout(width, height)
    }

    private fun gatherAppliedFilters(): Filters {
        val radioGroupSorting = binding.radioGroupSorting
        val ascendingOrder = radioGroupSorting.findViewById<RadioButton>(R.id.radioAscending)?.isChecked ?: false
        val descendingOrder = radioGroupSorting.findViewById<RadioButton>(R.id.radioDescending)?.isChecked ?: false

        val checkboxExcludeSoldOut = binding.checkboxExcludeSoldOut
        val excludeSoldOut = checkboxExcludeSoldOut?.isChecked ?: false

        val editTextMinPrice = binding.editMinPrice
        val minPrice = editTextMinPrice?.text.toString().toIntOrNull() ?: 0

        val editTextMaxPrice = binding.editMaxPrice
        val maxPrice = editTextMaxPrice?.text.toString().toIntOrNull() ?: Int.MAX_VALUE

        return Filters(
            ascendingOrder,
            descendingOrder,
            excludeSoldOut,
            minPrice,
            maxPrice
        )
    }
}
