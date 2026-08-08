import type { Device, NotificationType } from '../types'

type Props = {
  devices: Device[]
  deviceId: string
  type: 'all' | NotificationType
  search: string
  onDeviceChange: (value: string) => void
  onTypeChange: (value: 'all' | NotificationType) => void
  onSearchChange: (value: string) => void
}

export function Filters({
  devices,
  deviceId,
  type,
  search,
  onDeviceChange,
  onTypeChange,
  onSearchChange,
}: Props) {
  return (
    <section className="filters" aria-label="Notification filters">
      <label>
        Device
        <select value={deviceId} onChange={(event) => onDeviceChange(event.target.value)}>
          <option value="all">All devices</option>
          {devices.map((device) => (
            <option key={device.id} value={device.id}>
              {device.device_name}{device.device_model ? ` · ${device.device_model}` : ''}
            </option>
          ))}
        </select>
      </label>

      <label>
        Type
        <select
          value={type}
          onChange={(event) => onTypeChange(event.target.value as 'all' | NotificationType)}
        >
          <option value="all">All types</option>
          <option value="sms">SMS</option>
          <option value="app">App notifications</option>
        </select>
      </label>

      <label className="search-label">
        Search
        <input
          type="search"
          value={search}
          placeholder="Sender, body, package…"
          onChange={(event) => onSearchChange(event.target.value)}
        />
      </label>
    </section>
  )
}
