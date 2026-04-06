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
