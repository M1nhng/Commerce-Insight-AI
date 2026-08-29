/**
 * features/export/components/ExportFilters.tsx
 *
 * Renders the filter panel for the currently selected report type. Each report
 * only sees and writes its own supported fields.
 */
import type { ExportFormState, ExportReportType } from '../types/export.types'
import { ProductExportFilters } from './ProductExportFilters'
import { CustomerExportFilters } from './CustomerExportFilters'
import { OrderExportFilters } from './OrderExportFilters'
import { RevenueExportFilters } from './RevenueExportFilters'
import { TopProductsExportFilters } from './TopProductsExportFilters'
import { AnalyticsExportFilters } from './AnalyticsExportFilters'

interface Props {
  reportType: ExportReportType
  value: ExportFormState
  onChange: (patch: Partial<ExportFormState>) => void
  disabled?: boolean
  onCustomerLabelChange?: (label: string | null) => void
}

export function ExportFilters({
  reportType,
  value,
  onChange,
  disabled,
  onCustomerLabelChange,
}: Props) {
  switch (reportType) {
    case 'PRODUCTS':
      return <ProductExportFilters value={value} onChange={onChange} disabled={disabled} />
    case 'CUSTOMERS':
      return <CustomerExportFilters value={value} onChange={onChange} disabled={disabled} />
    case 'ORDERS':
      return (
        <OrderExportFilters
          value={value}
          onChange={onChange}
          disabled={disabled}
          onCustomerLabelChange={onCustomerLabelChange}
        />
      )
    case 'REVENUE':
      return <RevenueExportFilters value={value} onChange={onChange} disabled={disabled} />
    case 'TOP_PRODUCTS':
      return <TopProductsExportFilters value={value} onChange={onChange} disabled={disabled} />
    case 'ORDER_ANALYTICS':
    case 'CUSTOMER_ANALYTICS':
    case 'PAYMENT_ANALYTICS':
      return <AnalyticsExportFilters value={value} onChange={onChange} disabled={disabled} />
  }
}
