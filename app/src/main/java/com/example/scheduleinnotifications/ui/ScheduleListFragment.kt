package com.example.scheduleinnotifications.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.scheduleinnotifications.R
import com.example.scheduleinnotifications.databinding.FragmentScheduleListBinding
import com.example.scheduleinnotifications.service.ScheduleNotificationService
import com.example.scheduleinnotifications.ui.adapter.ScheduleAdapter
import com.example.scheduleinnotifications.ui.viewmodel.ScheduleViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

/**
 * Главный экран: список расписаний.
 */
class ScheduleListFragment : Fragment() {

    private var _binding: FragmentScheduleListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewModel by activityViewModels()
    private lateinit var adapter: ScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSchedules()

        binding.fabAddSchedule.setOnClickListener { showAddScheduleDialog() }
    }

    private fun setupRecyclerView() {
        adapter = ScheduleAdapter(
            onToggle = { schedule, enabled ->
                viewModel.setEnabled(schedule.id, enabled)
                syncService()
            },
            onClick = { schedule ->
                viewModel.selectSchedule(schedule.id)
                findNavController().navigate(
                    R.id.action_scheduleListFragment_to_scheduleDetailFragment,
                    Bundle().apply {
                        putLong("scheduleId", schedule.id)
                        putString("scheduleName", schedule.name)
                    }
                )
            },
            onDelete = { schedule ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_schedule_title)
                    .setMessage(getString(R.string.delete_schedule_message, schedule.name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteSchedule(schedule)
                        syncService()
                        Snackbar.make(binding.root, R.string.schedule_deleted, Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        binding.rvSchedules.adapter = adapter
    }

    private fun observeSchedules() {
        viewModel.allSchedules.observe(viewLifecycleOwner) { schedules ->
            adapter.submitList(schedules)
            binding.tvEmpty.isVisible = schedules.isEmpty()
        }
    }

    private fun showAddScheduleDialog() {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.schedule_name_hint)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_schedule_title)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = input.text?.toString()?.trim()
                if (!name.isNullOrBlank()) {
                    viewModel.addSchedule(name)
                } else {
                    Snackbar.make(binding.root, R.string.name_cannot_be_empty, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Запускает или перезапускает Service, чтобы он обновил список активных расписаний */
    private fun syncService() {
        val intent = Intent(requireContext(), ScheduleNotificationService::class.java)
            .setAction(ScheduleNotificationService.ACTION_START)
        requireContext().startForegroundService(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
