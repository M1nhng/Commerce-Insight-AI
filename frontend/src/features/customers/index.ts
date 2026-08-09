/**
 * features/customers/index.ts — Public API for the customer feature module.
 */

// Pages
export { CustomersPage }       from './pages/CustomersPage'
export { CreateCustomerPage }  from './pages/CreateCustomerPage'
export { EditCustomerPage }    from './pages/EditCustomerPage'
export { CustomerDetailPage }  from './pages/CustomerDetailPage'
export { CustomerGroupsPage }  from './pages/CustomerGroupsPage'

// Components
export { CustomerStatusBadge } from './components/CustomerStatusBadge'
export { CustomerTable }       from './components/CustomerTable'
export { CustomerForm }        from './components/CustomerForm'
export { AddressCard }         from './components/AddressCard'
export { AddressForm }         from './components/AddressForm'
export { CustomerGroupForm }   from './components/CustomerGroupForm'

// Hooks
export * from './hooks/useCustomers'
export * from './hooks/useCustomerGroups'
export * from './hooks/useCustomerAddresses'
