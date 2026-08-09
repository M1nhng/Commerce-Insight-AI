/**
 * Shared TypeScript types for the MCP Server.
 *
 * These interfaces mirror the backend DTO contracts.
 * Update in sync with any backend DTO changes.
 */

// ── Standard API envelope ─────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success:   boolean;
  data:      T;
  message:   string;
  timestamp: string;
}

export interface PageResponse<T> {
  content:          T[];
  totalElements:    number;
  totalPages:       number;
  number:           number;   // current page (0-indexed)
  size:             number;
  first:            boolean;
  last:             boolean;
  numberOfElements: number;
}

// ── Customer domain types ─────────────────────────────────────────────────────

export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
export type CustomerGender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';
export type AddressType    = 'SHIPPING' | 'BILLING';
export type GroupStatus    = 'ACTIVE' | 'INACTIVE';

export interface CustomerSummary {
  id:           string;
  customerCode: string;
  fullName:     string;
  firstName:    string;
  lastName:     string;
  email:        string | null;
  phone:        string | null;
  status:       CustomerStatus;
  groupId:      string | null;
  groupName:    string | null;
  createdAt:    string;
}

export interface CustomerAddress {
  id:            string;
  type:          AddressType;
  recipientName: string;
  phone:         string | null;
  addressLine:   string;
  ward:          string | null;
  district:      string | null;
  province:      string | null;
  country:       string;
  isDefault:     boolean;
}

export interface CustomerProfile extends CustomerSummary {
  dateOfBirth: string | null;
  gender:      CustomerGender | null;
  addresses:   CustomerAddress[];
  updatedAt:   string;
}

export interface CustomerGroup {
  id:          string;
  code:        string;
  name:        string;
  description: string | null;
  status:      GroupStatus;
  createdAt:   string;
  updatedAt:   string;
}

