package com.liveasadodo.conversationgenerator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.liveasadodo.conversationgenerator.data.database.ConversationEntity
import com.liveasadodo.conversationgenerator.databinding.ActivityHistoryBinding
import com.liveasadodo.conversationgenerator.ui.adapter.ConversationHistoryAdapter
import com.liveasadodo.conversationgenerator.ui.base.BaseActivity
import com.liveasadodo.conversationgenerator.ui.viewmodel.HistoryViewModel
import com.liveasadodo.conversationgenerator.ui.viewmodel.HistoryViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: ConversationHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        val factory = HistoryViewModelFactory(historyRepository)
        viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = ConversationHistoryAdapter(
            onItemClick = { conversation ->
                // Open conversation detail
                val intent = Intent(this, ConversationDetailActivity::class.java).apply {
                    putExtra("CONVERSATION_ID", conversation.id)
                }
                startActivity(intent)
            },
            onFavoriteClick = { conversation ->
                viewModel.toggleFavorite(conversation)
            },
            onDeleteClick = { conversation ->
                showDeleteConfirmation(conversation)
            }
        )

        binding.conversationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.conversationsRecyclerView.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.conversations.observe(this) { conversations ->
            adapter.submitList(conversations)

            // Show/hide empty state
            if (conversations.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.conversationsRecyclerView.visibility = View.GONE

                // Update empty state message based on filter
                val showFavorites = viewModel.showFavoritesOnly.value ?: false
                binding.emptyStateText.text = if (showFavorites) {
                    getString(R.string.message_no_favorites)
                } else {
                    getString(R.string.message_no_conversations)
                }
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.conversationsRecyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.showFavoritesOnly.observe(this) { showFavorites ->
            binding.filterButton.text = if (showFavorites) {
                getString(R.string.button_show_all)
            } else {
                getString(R.string.button_filter_favorites)
            }
        }
    }

    private fun setupListeners() {
        binding.filterButton.setOnClickListener {
            viewModel.toggleFilter()
        }
    }

    private fun showDeleteConfirmation(conversation: ConversationEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.deleteConversation(conversation)
                Toast.makeText(this, R.string.message_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }
}
