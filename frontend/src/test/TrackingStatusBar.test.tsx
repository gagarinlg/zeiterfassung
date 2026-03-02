import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import TrackingStatusBar from '../components/TrackingStatusBar'
import type { TrackingStatusResponse } from '../types'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'de', changeLanguage: vi.fn() },
  }),
}))

vi.mock('../utils/dateUtils', () => ({
  formatTime: (ts: string) => ts,
}))

const baseProps = {
  timeFormat: 'HH:mm',
  actionLoading: false,
  actionError: null,
  onClockIn: vi.fn(),
  onClockOut: vi.fn(),
  onStartBreak: vi.fn(),
  onEndBreak: vi.fn(),
}

function makeStatus(overrides: Partial<TrackingStatusResponse> = {}): TrackingStatusResponse {
  return {
    status: 'CLOCKED_OUT',
    clockedInSince: null,
    breakStartedAt: null,
    elapsedWorkMinutes: 0,
    elapsedBreakMinutes: 0,
    todayWorkMinutes: 0,
    todayBreakMinutes: 0,
    ...overrides,
  }
}

describe('TrackingStatusBar', () => {
  it('should render current status label', () => {
    render(<TrackingStatusBar {...baseProps} status={makeStatus()} />)
    expect(screen.getByText('time_tracking.current_status')).toBeInTheDocument()
    expect(screen.getByText('time_tracking.status.clocked_out')).toBeInTheDocument()
  })

  it('should show Clock In button when CLOCKED_OUT', () => {
    render(<TrackingStatusBar {...baseProps} status={makeStatus()} />)
    expect(screen.getByRole('button', { name: 'time_tracking.clock_in' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'time_tracking.clock_out' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'time_tracking.break_start' })).not.toBeInTheDocument()
  })

  it('should show Start Break and Clock Out buttons when CLOCKED_IN', () => {
    render(<TrackingStatusBar {...baseProps} status={makeStatus({ status: 'CLOCKED_IN' })} />)
    expect(screen.getByRole('button', { name: 'time_tracking.break_start' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'time_tracking.clock_out' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'time_tracking.clock_in' })).not.toBeInTheDocument()
  })

  it('should show End Break button but NOT Clock Out button when ON_BREAK', () => {
    render(<TrackingStatusBar {...baseProps} status={makeStatus({ status: 'ON_BREAK' })} />)
    expect(screen.getByRole('button', { name: 'time_tracking.break_end' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'time_tracking.clock_out' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'time_tracking.clock_in' })).not.toBeInTheDocument()
  })

  it('should use green background when CLOCKED_IN', () => {
    const { container } = render(
      <TrackingStatusBar {...baseProps} status={makeStatus({ status: 'CLOCKED_IN' })} />
    )
    const wrapper = container.firstChild as HTMLElement
    expect(wrapper.className).toContain('bg-green-50')
    expect(wrapper.className).toContain('border-green-200')
  })

  it('should use yellow background when ON_BREAK', () => {
    const { container } = render(
      <TrackingStatusBar {...baseProps} status={makeStatus({ status: 'ON_BREAK' })} />
    )
    const wrapper = container.firstChild as HTMLElement
    expect(wrapper.className).toContain('bg-yellow-50')
    expect(wrapper.className).toContain('border-yellow-200')
  })

  it('should use neutral background when CLOCKED_OUT', () => {
    const { container } = render(
      <TrackingStatusBar {...baseProps} status={makeStatus({ status: 'CLOCKED_OUT' })} />
    )
    const wrapper = container.firstChild as HTMLElement
    expect(wrapper.className).toContain('bg-white')
    expect(wrapper.className).toContain('border-gray-200')
  })

  it('should show clocked in since time when CLOCKED_IN', () => {
    render(
      <TrackingStatusBar
        {...baseProps}
        status={makeStatus({ status: 'CLOCKED_IN', clockedInSince: '2026-03-02T08:00:00Z' })}
      />
    )
    expect(screen.getByText(/dashboard\.clocked_in_since/)).toBeInTheDocument()
  })

  it('should show on break since time when ON_BREAK', () => {
    render(
      <TrackingStatusBar
        {...baseProps}
        status={makeStatus({ status: 'ON_BREAK', breakStartedAt: '2026-03-02T12:00:00Z' })}
      />
    )
    expect(screen.getByText(/dashboard\.on_break_since/)).toBeInTheDocument()
  })

  it('should call onClockIn when Clock In button is clicked', () => {
    const onClockIn = vi.fn()
    render(<TrackingStatusBar {...baseProps} onClockIn={onClockIn} status={makeStatus()} />)
    fireEvent.click(screen.getByRole('button', { name: 'time_tracking.clock_in' }))
    expect(onClockIn).toHaveBeenCalledTimes(1)
  })

  it('should call onEndBreak when End Break button is clicked', () => {
    const onEndBreak = vi.fn()
    render(
      <TrackingStatusBar {...baseProps} onEndBreak={onEndBreak} status={makeStatus({ status: 'ON_BREAK' })} />
    )
    fireEvent.click(screen.getByRole('button', { name: 'time_tracking.break_end' }))
    expect(onEndBreak).toHaveBeenCalledTimes(1)
  })

  it('should disable buttons when actionLoading is true', () => {
    render(
      <TrackingStatusBar {...baseProps} actionLoading={true} status={makeStatus({ status: 'CLOCKED_IN' })} />
    )
    expect(screen.getByRole('button', { name: 'time_tracking.break_start' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'time_tracking.clock_out' })).toBeDisabled()
  })

  it('should show action error when provided', () => {
    render(
      <TrackingStatusBar {...baseProps} actionError="Something went wrong" status={makeStatus()} />
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong')
  })

  it('should not show action error when null', () => {
    render(<TrackingStatusBar {...baseProps} actionError={null} status={makeStatus()} />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('should use green color for End Break button (resume working)', () => {
    render(
      <TrackingStatusBar {...baseProps} status={makeStatus({ status: 'ON_BREAK' })} />
    )
    const endBreakBtn = screen.getByRole('button', { name: 'time_tracking.break_end' })
    expect(endBreakBtn.className).toContain('bg-green-600')
  })
})
