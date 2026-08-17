/**
 * Orders Feature — Public API
 */
export { OrdersPage }      from './pages/OrdersPage'
export { OrderDetailPage } from './pages/OrderDetailPage'
export { CreateOrderPage } from './pages/CreateOrderPage'

export { OrderTable }          from './components/OrderTable'
export { OrderStatusBadge }    from './components/OrderStatusBadge'
export { PaymentStatusBadge }  from './components/PaymentStatusBadge'
export { OrderStatusTimeline } from './components/OrderStatusTimeline'
export { OrderItemsTable }     from './components/OrderItemsTable'
export { OrderAddressCard }    from './components/OrderAddressCard'
export { CreateOrderForm }     from './components/CreateOrderForm'

export { useOrders, useOrder, useCreateOrder, useUpdateOrderStatus, useCancelOrder, ORDER_KEYS } from './hooks/useOrders'
export { orderService } from './services/orderService'
