package com.samsunghub.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class IncentiveFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val tv = TextView(context)
        tv.text = "Incentive Calculator (Coming Soon)"
        tv.textSize = 24f
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return tv
    }
}
