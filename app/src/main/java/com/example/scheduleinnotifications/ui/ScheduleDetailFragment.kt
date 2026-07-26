package com.example.scheduleinnotifications.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.scheduleinnotifications.R
import com.example.scheduleinnotifications.databinding.FragmentScheduleDetailBinding
import com.example.scheduleinnotifications.ui.adapter.LessonAdapter
import com.example.scheduleinnotifications.ui.viewmodel.ScheduleViewModel
import com.example.scheduleinnotifications.util.DateUtils
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/**
 * Экран деталей расписания.
 * Сверху — чипы дней недели (Пн–Вс), снизу — список уроков для выбранного дня.
 * Фильтрация по дню выполняется в ScheduleViewModel через lessonsForCurrentDay.
 */
class ScheduleDetailFragment : Fragment() {

    private var _binding: FragmentScheduleDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewModel by activityViewModels()
    private lateinit var lessonAdapter: LessonAdapter

    // Аргументы — читаем через SafeArgs (типобезопасно, без хардкода ключей)
    private val args: ScheduleDetailFragmentArgs by lazy {
        ScheduleDetailFragmentArgs.fromBundle(requireArguments())
    }

    private val dayLabels get() = resources.getStringArray(R.array.days_of_week).toList()

    // Импорт файла
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val text = requireContext().contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.readText()
                    ?: return@let
                viewModel.importLessonsFromCsv(args.scheduleId, text)
                Snackbar.make(binding.root, R.string.import_success, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets()

        // Заголовок
        binding.tvScheduleDetailTitle.text = args.scheduleName

        // Чипы дней недели
        setupDayChips()

        // Список уроков
        lessonAdapter = LessonAdapter { lesson ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_lesson_title)
                .setMessage(getString(R.string.delete_lesson_message, lesson.name))
                .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteLesson(lesson) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.rvLessons.adapter = lessonAdapter

        // Подписка на уроки — фильтрация по дню выполняется в ViewModel
        viewModel.lessonsForCurrentDay.observe(viewLifecycleOwner) { filtered ->
            lessonAdapter.submitList(filtered)
            binding.tvNoLessons.isVisible = filtered.isEmpty()
        }

        // Кнопка добавить урок
        binding.fabAddLesson.setOnClickListener { showAddLessonDialog() }

        // Кнопка импорт
        binding.btnImport.setOnClickListener { openFilePicker() }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val margin16 = (16 * resources.displayMetrics.density).toInt()
            val padding88 = (88 * resources.displayMetrics.density).toInt()
            val padding8 = (8 * resources.displayMetrics.density).toInt()

            val fabParams = binding.fabAddLesson.layoutParams as ViewGroup.MarginLayoutParams
            fabParams.bottomMargin = navBars.bottom + margin16
            binding.fabAddLesson.layoutParams = fabParams

            binding.rvLessons.setPadding(0, padding8, 0, navBars.bottom + padding88)
            insets
        }
    }

    private fun setupDayChips() {
        val chipGroup = binding.chipGroupDays

        // Инициируем выбор сегодняшнего дня в ViewModel (DateUtils — единая точка логики)
        val todayLocal = DateUtils.todayLocal()
        viewModel.selectDay(todayLocal)

        dayLabels.forEachIndexed { index, label ->
            val day = index + 1
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = (day == todayLocal)
                id = View.generateViewId()
            }
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    // Передаём выбор дня в ViewModel — фрагмент не фильтрует сам
                    viewModel.selectDay(day)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun showAddLessonDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_lesson, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_lesson_name)

        var startMinute = 8 * 60
        var endMinute = 8 * 60 + 40

        val tvStart = dialogView.findViewById<android.widget.TextView>(R.id.tv_start_time)
        val tvEnd = dialogView.findViewById<android.widget.TextView>(R.id.tv_end_time)

        fun updateTimeViews() {
            tvStart.text = "%02d:%02d".format(startMinute / 60, startMinute % 60)
            tvEnd.text = "%02d:%02d".format(endMinute / 60, endMinute % 60)
        }
        updateTimeViews()

        tvStart.setOnClickListener {
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(startMinute / 60)
                .setMinute(startMinute % 60)
                .setTitleText(R.string.pick_start_time)
                .build()
                .also { picker ->
                    picker.addOnPositiveButtonClickListener {
                        startMinute = picker.hour * 60 + picker.minute
                        updateTimeViews()
                    }
                    picker.show(childFragmentManager, "start_picker")
                }
        }

        tvEnd.setOnClickListener {
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(endMinute / 60)
                .setMinute(endMinute % 60)
                .setTitleText(R.string.pick_end_time)
                .build()
                .also { picker ->
                    picker.addOnPositiveButtonClickListener {
                        endMinute = picker.hour * 60 + picker.minute
                        updateTimeViews()
                    }
                    picker.show(childFragmentManager, "end_picker")
                }
        }

        // Берём текущий день из ViewModel — единственный источник истины
        val currentDay = viewModel.selectedDay.value ?: DateUtils.todayLocal()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_lesson_title)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = etName.text?.toString()?.trim()
                if (name.isNullOrBlank()) {
                    Snackbar.make(binding.root, R.string.name_cannot_be_empty, Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (endMinute <= startMinute) {
                    Snackbar.make(binding.root, R.string.end_before_start_error, Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.addLesson(args.scheduleId, name, currentDay, startMinute, endMinute)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/csv", "application/octet-stream"))
        }
        importFileLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
