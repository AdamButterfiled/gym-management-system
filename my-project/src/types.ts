// Common Types

export interface Venue {
    id: number;
    name: string;
    location: string;
    capacity: number;
    status: number; // 0-Closed, 1-Open
    openTime?: string;
    closeTime?: string;
    image?: string;
}

export interface User {
    id: number;
    username: string;
    realName: string;
    nickname?: string;
    email?: string;
    role: 'ADMIN' | 'STAFF' | 'MEMBER' | 'COACH';
    phone: string;
    avatar?: string;
    balance?: number;
    type?: string; // REGULAR, VIP
    status?: string;
    createdAt?: string;
}

export interface Reservation {
    id: number;
    userId: number;
    targetId: number;
    targetType: 'VENUE' | 'EQUIPMENT' | 'COURSE';
    startTime: string; // ISO String
    endTime: string;
    status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'CHECKED_IN';
    createdAt: string;
}

export interface Course {
    id: number;
    name: string;
    coachId?: number;
    venueId?: number;
    startTime: string;
    endTime: string;
    maxParticipants: number;
    currentParticipants: number;
    description?: string;
}

export interface Equipment {
    id: number;
    name: string;
    description: string;
    status: 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE';
    venueId: number;
    image?: string;
    quantity?: number;
}

export interface Repair {
    id: number;
    equipmentId?: number;
    venueId?: number;
    reporterId?: number;
    description: string;
    status: 'PENDING' | 'PROCESSING' | 'FIXED';
    createdAt: string;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
}

export interface AdminOverview {
    memberCount: number;
    coachCount: number;
    todayBookings: number;
    pendingCoachApprovals: number;
    totalRevenue: number;
    todayRevenue: number;
    venueUsageRate: number;
    courseAttendanceRate: number;
    checkinRate: number;
}

export interface VenueResource {
    id?: number;
    name: string;
    location: string;
    capacity: number;
    description?: string;
    status: number;
    openTime?: string;
    closeTime?: string;
    image?: string;
    coverImage?: string;
    layoutJson?: string;
    pricePerHour?: number;
}

export interface CoachProfile {
    id?: number;
    userId?: number;
    name: string;
    gender?: number;
    age?: number;
    phone?: string;
    specialization?: string;
    entryDate?: string;
    intro?: string;
    status?: number;
    avatar?: string;
    hourlyPrice?: number;
    rating?: number;
}

export interface CourseSchedule {
    id?: number;
    name: string;
    coachId?: number;
    venueId?: number;
    startTime: string;
    endTime: string;
    capacity: number;
    bookedCount?: number;
    normalPrice?: number;
    flashSale?: number;
    flashSalePrice?: number;
    description?: string;
    status?: string;
}

export interface PrivateSchedule {
    id?: number;
    coachId?: number;
    venueId?: number;
    startTime: string;
    endTime: string;
    capacity?: number;
    bookedCount?: number;
    description?: string;
    status?: string;
}

export interface BookingOrder {
    id: number;
    orderNo: string;
    userId: number;
    resourceType: 'VENUE' | 'GROUP_COURSE' | 'PRIVATE_COACH';
    resourceId: number;
    resourceName: string;
    venueId?: number;
    coachId?: number;
    bookingDate: string;
    startTime: string;
    endTime: string;
    amount: number;
    paymentStatus: 'UNPAID' | 'PAID' | 'CLOSED' | 'REFUNDED';
    status: 'CREATED' | 'PENDING_PAY' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'CHECKED_IN' | 'REFUNDED';
    source?: string;
    checkedInAt?: string;
    remark?: string;
    createdAt?: string;
}

export interface PaymentOrder {
    id: number;
    paymentNo: string;
    userId: number;
    paymentType: 'RECHARGE' | 'MEMBERSHIP' | 'PRIVATE_PACKAGE' | 'BOOKING';
    targetType: string;
    targetId?: number;
    amount: number;
    status: 'UNPAID' | 'PAID' | 'CLOSED' | 'REFUNDED';
    payloadJson?: string;
    paidAt?: string;
    createdAt?: string;
}

export interface CheckinRecord {
    id: number;
    bookingOrderId: number;
    userId: number;
    checkinCode: string;
    checkinTime: string;
    status: string;
    operatorName: string;
}

export interface ScheduleConflict {
    id: number;
    resourceType: string;
    resourceId?: number;
    conflictType: string;
    referenceId?: number;
    message: string;
    startTime: string;
    endTime: string;
    createdAt?: string;
}

export interface MembershipPackage {
    id?: number;
    name: string;
    price: number;
    days: number;
    dailyLimit?: number;
    description?: string;
    status?: string;
}

export interface PrivatePackage {
    id?: number;
    coachId?: number;
    name: string;
    price: number;
    totalSessions: number;
    description?: string;
    status?: string;
}

export interface MemberAssetRow {
    userId: number;
    username: string;
    realName: string;
    balance: number;
    membership?: string;
    membershipEndDate?: string;
    remainingPrivateSessions: number;
}
