package com.liveasadodo.conversationgenerator.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.liveasadodo.conversationgenerator.R
import com.liveasadodo.conversationgenerator.data.model.Language

class LanguageSpinnerAdapter(
    context: Context,
    private val languages: List<Language>
) : ArrayAdapter<Language>(context, 0, languages) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent, false)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent, true)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup, isDropdown: Boolean): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            if (isDropdown) android.R.layout.simple_spinner_dropdown_item
            else android.R.layout.simple_spinner_item,
            parent,
            false
        )

        val textView = view.findViewById<TextView>(android.R.id.text1)
        val language = languages[position]

        textView.text = "${language.flag} ${language.displayName}"

        return view
    }
}
