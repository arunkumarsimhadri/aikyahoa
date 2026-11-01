export type LocaleCode = 'en' | 'te';

export type Role =
  | 'SUPER_ADMIN'
  | 'PRESIDENT'
  | 'SECRETARY'
  | 'TREASURER'
  | 'COMMITTEE_MEMBER'
  | 'OWNER'
  | 'TENANT'
  | 'SECURITY_STAFF'
  | 'TECHNICIAN';

export type BillingType = 'AREA_BASED' | 'FLAT_RATE' | 'HYBRID';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface UserDto {
  id: number;
  phoneNumber: string;
  fullName: string;
  fullNameTe?: string;
  email?: string;
  preferredLocale: LocaleCode;
  active: boolean;
}

export interface AssociationMembershipDto {
  membershipId: number;
  associationId: number;
  associationName: string;
  associationNameTe?: string;
  associationCode: string;
  schemaName: string;
  role: Role;
  unitNumber?: string;
  blockName?: string;
}

export interface AuthTokenResponse {
  accessToken: string;
  tokenType: string;
  user: UserDto;
  activeAssociation: AssociationMembershipDto;
  memberships: AssociationMembershipDto[];
}

export interface OtpSendResponse {
  phoneNumber: string;
  expiresInSeconds: number;
  mockOtpCode?: string;
}

export interface AssociationDto {
  id: number;
  name: string;
  nameTe?: string;
  code: string;
  schemaName: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  billingType: BillingType;
  ratePerSqft: number;
  flatMaintenanceAmount: number;
  sinkingFundAmount: number;
  waterChargePerUnit: number;
  lateFeePercentage: number;
  active: boolean;
}

export interface UnitDto {
  id: number;
  blockName: string;
  unitNumber: string;
  floorNumber: number;
  areaSqft: number;
  bhkType: string;
  ownerUserId?: number;
  ownerName?: string;
  ownerPhone?: string;
  tenantUserId?: number;
  tenantName?: string;
  tenantPhone?: string;
  occupancyStatus: 'OWNER_OCCUPIED' | 'TENANT_OCCUPIED' | 'VACANT';
  intercomNumber?: string;
  parkingSlots?: string;
}

export interface InvoiceDto {
  id: number;
  invoiceNumber: string;
  unitId: number;
  unitNumber: string;
  blockName: string;
  areaSqft: number;
  billingMonth: number;
  billingYear: number;
  dueDate: string;
  maintenanceCharge: number;
  sinkingFundCharge: number;
  waterCharge: number;
  otherCharges: number;
  lateFee: number;
  totalAmount: number;
  paidAmount: number;
  status: 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';
  notes?: string;
  createdAt: string;
}

export interface PaymentDto {
  id: number;
  invoiceId: number;
  invoiceNumber?: string;
  unitId: number;
  unitNumber?: string;
  blockName?: string;
  payerUserId?: number;
  payerName?: string;
  amount: number;
  paymentMethod: 'RAZORPAY' | 'CASHFREE' | 'CHEQUE' | 'NEFT' | 'IMPS' | 'UPI_MANUAL';
  gatewayName?: string;
  gatewayOrderId?: string;
  gatewayPaymentId?: string;
  referenceNumber?: string;
  proofFileUrl?: string;
  proofFileUrls?: string[];
  reconciliationStatus: 'PENDING_VERIFICATION' | 'VERIFIED' | 'REJECTED';
  reconciledByUserId?: number;
  reconciledByName?: string;
  reconciledAt?: string;
  auditNotes?: string;
  paidAt: string;
}

export interface HelpdeskTicketDto {
  id: number;
  ticketNumber: string;
  unitId?: number;
  unitNumber?: string;
  raisedByUserId: number;
  raisedByName?: string;
  category: 'PLUMBING' | 'ELECTRICAL' | 'ELEVATOR' | 'SECURITY' | 'HOUSEKEEPING' | 'CARPENTRY' | 'WATER_SUPPLY' | 'OTHER';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  title: string;
  description: string;
  attachmentUrl?: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  assignedToUserId?: number;
  assignedTechnicianName?: string;
  assignedTechnicianPhone?: string;
  slaDueAt: string;
  slaBreached: boolean;
  resolutionNotes?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
}

export interface AmenityDto {
  id: number;
  name: string;
  nameTe?: string;
  code: string;
  description?: string;
  descriptionTe?: string;
  capacity: number;
  hourlyRate: number;
  openTime: string;
  closeTime: string;
  maxHoursPerBooking: number;
  active: boolean;
}

export interface AmenitySlotDto {
  startTime: string;
  endTime: string;
  available: boolean;
  bookedByUnit?: string;
  bookingId?: number;
}

export interface AmenityBookingDto {
  id: number;
  amenityId: number;
  amenityName: string;
  amenityNameTe?: string;
  unitId: number;
  unitNumber: string;
  bookedByUserId: number;
  bookedByName?: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  purpose?: string;
  guestCount: number;
  feeAmount: number;
  status: 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
}

export interface PollOptionDto {
  id: number;
  optionText: string;
  optionTextTe?: string;
  displayOrder: number;
  voteCount: number;
  percentage: number;
  voters?: string[];
}

export interface PollDto {
  id: number;
  title: string;
  titleTe?: string;
  description?: string;
  descriptionTe?: string;
  isSecret: boolean;
  ownerOnly: boolean;
  status: 'ACTIVE' | 'CLOSED';
  startsAt: string;
  endsAt: string;
  totalVotes: number;
  hasUnitVoted: boolean;
  votedOptionId?: number;
  options: PollOptionDto[];
  createdByName?: string;
  createdAt: string;
}

export interface DocumentDto {
  id: number;
  title: string;
  titleTe?: string;
  category: 'AGM_MINUTES' | 'AUDIT_REPORT' | 'BYLAWS' | 'CIRCULAR' | 'NOC_TEMPLATE';
  financialYear: string;
  fileUrl: string;
  fileSizeBytes: number;
  uploadedByName?: string;
  createdAt: string;
}

export interface NoticeDto {
  id: number;
  title: string;
  titleTe?: string;
  content: string;
  contentTe?: string;
  category: 'GENERAL' | 'MAINTENANCE' | 'EVENT' | 'EMERGENCY' | 'AGM';
  priority: 'NORMAL' | 'HIGH' | 'URGENT';
  pinned: boolean;
  publishedByName?: string;
  publishedAt: string;
  expiresAt?: string;
}

export interface NotificationLogDto {
  id: number;
  recipientUserId?: number;
  recipientPhone: string;
  recipientName?: string;
  channel: 'SMS' | 'WHATSAPP';
  templateKey: string;
  locale: LocaleCode;
  renderedMessage: string;
  status: string;
  createdAt: string;
}

export interface FileAttachmentDto {
  id?: number;
  entityType: 'MAINTENANCE_PAYMENT' | 'ASSOCIATION_EXPENSE' | 'HELPDESK_TICKET' | 'AGM_DOCUMENT';
  entityId?: number;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
  storageProvider: string;
  storagePath?: string;
  fileUrl: string;
  uploadedByName?: string;
  createdAt?: string;
}

export interface MaintenanceExpenseDto {
  id: number;
  expenseNumber: string;
  category:
    | 'ELEVATOR_AMC'
    | 'GENERATOR_DIESEL'
    | 'WATER_TANKER'
    | 'SECURITY_AGENCY'
    | 'HOUSEKEEPING'
    | 'ELECTRICITY_COMMON'
    | 'PLUMBING_REPAIR'
    | 'FESTIVAL_EVENT'
    | 'OTHER';
  title: string;
  titleTe?: string;
  vendorName: string;
  amount: number;
  expenseDate: string;
  paymentMode: 'NEFT' | 'IMPS' | 'UPI' | 'CHEQUE' | 'CASH';
  transactionReference?: string;
  receiptFileUrls: string[];
  attachments?: FileAttachmentDto[];
  recordedByName?: string;
  approvedByName?: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'PAID';
  notes?: string;
  createdAt: string;
}

export interface DashboardSummaryDto {
  totalOutstandingDues: number;
  pendingInvoicesCount: number;
  unreconciledPaymentsCount: number;
  totalCollectionThisMonth: number;
  openTicketsCount: number;
  slaBreachedTicketsCount: number;
  upcomingAmenityBookingsCount: number;
  activePollsCount: number;
  unitDetails?: UnitDto;
  recentInvoices: InvoiceDto[];
  activeTickets: HelpdeskTicketDto[];
  recentNotices: NoticeDto[];
}

