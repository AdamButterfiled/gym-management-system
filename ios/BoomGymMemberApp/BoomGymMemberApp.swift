import AVFoundation
import CoreImage.CIFilterBuiltins
import ImageIO
import SwiftUI
import UIKit

@main
struct BoomGymMemberApp: App {
    @StateObject private var store = MemberStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
        }
    }
}

private let imageCache = URLCache(memoryCapacity: 24 * 1024 * 1024, diskCapacity: 120 * 1024 * 1024, diskPath: "boomgym-image-cache")

private func displayImage(from data: Data, maxPixelSize: CGFloat = 1200) -> UIImage? {
    let sourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
    guard let source = CGImageSourceCreateWithData(data as CFData, sourceOptions) else {
        return UIImage(data: data)
    }

    let options = [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        kCGImageSourceCreateThumbnailWithTransform: true,
        kCGImageSourceShouldCacheImmediately: true,
        kCGImageSourceThumbnailMaxPixelSize: maxPixelSize
    ] as CFDictionary

    guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, options) else {
        return UIImage(data: data)
    }
    return UIImage(cgImage: cgImage)
}

private func decodedDisplayImage(from data: Data) async -> UIImage? {
    await Task.detached(priority: .utility) {
        displayImage(from: data)
    }.value
}

private enum Palette {
    static let page = Color(red: 0.965, green: 0.965, blue: 0.965)
    static let card = Color.white
    static let text = Color(red: 0.20, green: 0.20, blue: 0.20)
    static let muted = Color(red: 0.49, green: 0.49, blue: 0.49)
    static let line = Color(red: 0.91, green: 0.91, blue: 0.91)
    static let main = Color(red: 0.631, green: 0.918, blue: 0.169)
    static let mainSoft = Color(red: 0.631, green: 0.918, blue: 0.169).opacity(0.20)
    static let orange = Color(red: 1.0, green: 0.455, blue: 0.153)
}

private enum OriginalAssets {
    static let storeLogo = URL(string: "https://imagecdn.rocketbird.cn/online/image/d7ec98432784fc12f5dbf15bb28f6e33.png")
    static let homeBanner = URL(string: "https://imagecdn.rocketbird.cn/minprogram/uni-member/banner.png")
    static let courseHero = URL(string: "https://imagecdn.rocketbird.cn/online/image/ebe61ccd6c0c1cf4138fd7b4d9ff203c.png")

    private static let homeCoachAvatars = [
        "https://imagecdn.rocketbird.cn/online/image/4281d7e958ea7924f703710a7bd80166.png",
        "https://imagecdn.rocketbird.cn/online/image/a1e44b04a130f55f6abe7afb3e69d624.png",
        "https://imagecdn.rocketbird.cn/online/image/17f8ece8ce0c44d49eb8d395835b93a5.png"
    ]

    private static let coachAvatarByName = [
        "Ziel": "https://imagecdn.rocketbird.cn/online/image/0cf7002bbf7bec552ff7f5d5ca73878d.png",
        "普拉提精炼": "https://imagecdn.rocketbird.cn/online/image/40136c88191fbb03be83705b6657f1fb.png",
        "Eurus": "https://imagecdn.rocketbird.cn/online/image/32cfcf720e53899a09a5f04b68aa669e.png",
        "大超Ray": "https://imagecdn.rocketbird.cn/online/image/9fdf3886f729caa1be2de8ed2605678031461c.png",
        "Merida": "https://imagecdn.rocketbird.cn/online/image/c48f35eb39b63288d496c50e5162223d.png",
        "Trista": "https://imagecdn.rocketbird.cn/online/image/21d6918e84046275261ad6e945a0a953.png",
        "Cynthia": "https://imagecdn.rocketbird.cn/online/image/06035c08eff2a5eed98e5e46b342ec80.png",
        "Kiwi": "https://imagecdn.rocketbird.cn/online/image/9b0c1e3ee01110361ba6742aa1bd4899.png",
        "Zendaya": "https://imagecdn.rocketbird.cn/online/image/fc41bc5ed414174914f614a416f8c821.png",
        "Ss": "https://imagecdn.rocketbird.cn/online/image/bb98510e82a20eb3ffbdfd2d493d6fec.png",
        "Wayne": "https://imagecdn.rocketbird.cn/online/image/b145add982c076aa6af3925cfe7375fc.png",
        "Sandy": "https://imagecdn.rocketbird.cn/online/image/15d4dface9be2de8ed2605678031461c.png",
        "常琪": "https://imagecdn.rocketbird.cn/online/image/6732a95805fab72aefef18866f1b2228.png"
    ]

    private static let packageCovers = [
        "https://imagecdn.rocketbird.cn/mainsite-fe/diy/card-cover-1.png",
        "https://imagecdn.rocketbird.cn/online/image/bc66ae2bffa1809542e865df1d02fc6a.png"
    ]

    private static let venueCovers = [
        "https://imagecdn.rocketbird.cn/online/image/d7ec98432784fc12f5dbf15bb28f6e33.png",
        "https://imagecdn.rocketbird.cn/online/image/a5a9cc341284af64b6c0953823c83511.png",
        "https://imagecdn.rocketbird.cn/online/image/cbc8e9a8630a8e34deea1c0bc711bb8c.png"
    ]

    private static let memberAvatars = [
        "https://imagecdn.rocketbird.cn/online/image/0cf7002bbf7bec552ff7f5d5ca73878d.png",
        "https://imagecdn.rocketbird.cn/online/image/40136c88191fbb03be83705b6657f1fb.png",
        "https://imagecdn.rocketbird.cn/online/image/c48f35eb39b63288d496c50e5162223d.png"
    ]

    static func coachAvatar(name: String?, index: Int) -> URL? {
        if let name, let value = coachAvatarByName[name] {
            return URL(string: value)
        }
        return url(from: homeCoachAvatars, index: index)
    }

    static func courseAvatar(coachName: String?, index: Int) -> URL? {
        coachAvatar(name: coachName, index: index)
    }

    static func packageCover(index: Int) -> URL? {
        url(from: packageCovers, index: index)
    }

    static func memberAvatar(index: Int) -> URL? {
        url(from: memberAvatars, index: index)
    }

    static func venueCover(index: Int) -> URL? {
        url(from: venueCovers, index: index)
    }

    private static func url(from values: [String], index: Int) -> URL? {
        guard !values.isEmpty else { return nil }
        let safeIndex = ((index % values.count) + values.count) % values.count
        return URL(string: values[safeIndex])
    }
}

private let homeGymDisplayName = "华北水利水电大学绿魔健身"
private let homeGymNavName = "绿魔 | 华水店"
private let homeGymAddress = "华北水利水电大学龙子湖校区 · 文体中心"

private struct RootView: View {
    @EnvironmentObject private var store: MemberStore

    var body: some View {
        ZStack {
            Palette.page.ignoresSafeArea()
            if store.session == nil {
                LoginView()
            } else if store.session?.isCoach == true {
                CoachShell()
            } else {
                MemberShell()
            }

            if store.isWorking {
                ProgressView()
                    .tint(Palette.text)
                    .padding(18)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
        }
        .foregroundStyle(Palette.text)
        .task { await store.bootstrap() }
        .alert("提示", isPresented: Binding(
            get: { store.alert != nil },
            set: { if !$0 { store.alert = nil } }
        )) {
            Button("知道了", role: .cancel) { store.alert = nil }
        } message: {
            Text(store.alert ?? "")
        }
        .overlay(alignment: .top) {
            if let toast = store.toast {
                Text(toast)
                    .font(.footnote.weight(.medium))
                    .foregroundStyle(Palette.text)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Palette.main, in: Capsule())
                    .padding(.top, 12)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.32, dampingFraction: 0.86), value: store.toast)
    }
}

private enum MemberSheet: Identifiable {
    case wallet
    case bookings
    case checkin
    case search

    var id: String {
        switch self {
        case .wallet: "wallet"
        case .bookings: "bookings"
        case .checkin: "checkin"
        case .search: "search"
        }
    }
}

private struct MemberShell: View {
    @State private var sheet: MemberSheet?
    @State private var showMine = false
    @State private var selectedTab: MemberTab = .home
    @State private var bookingSegment: BookingSegment = .course

    private func openBooking(_ segment: BookingSegment) {
        bookingSegment = segment
        selectedTab = .booking
        sheet = nil
    }

    var body: some View {
        Group {
            if #available(iOS 18.0, *) {
                NativeMemberTabView(
                    selection: $selectedTab,
                    bookingSegment: $bookingSegment,
                    sheet: $sheet,
                    openMine: { showMine = true },
                    openBooking: openBooking
                )
            } else {
                LegacyMemberTabView(
                    selection: $selectedTab,
                    bookingSegment: $bookingSegment,
                    sheet: $sheet,
                    openMine: { showMine = true },
                    openBooking: openBooking
                )
            }
        }
        .tint(Color(red: 0.02, green: 0.75, blue: 0.66))
        .sheet(item: $sheet) { sheet in
            switch sheet {
            case .wallet:
                WalletSheet()
            case .bookings:
                BookingsSheet()
            case .checkin:
                CheckinSheet(mode: .code)
            case .search:
                SearchSheet(sheet: $sheet, openBooking: openBooking)
            }
        }
        .fullScreenCover(isPresented: $showMine) {
            MineView(sheet: $sheet, close: { showMine = false })
        }
    }
}

private enum MemberTab: Hashable {
    case home
    case booking
    case checkin
    case records
    case search
}

private enum MemberTabIcon {
    static let home = "house"
    static let booking = "calendar.badge.plus"
    static let checkin = "qrcode.viewfinder"
    static let records = "calendar.badge.clock"
    static let search = "magnifyingglass"
}

@available(iOS 18.0, *)
private struct NativeMemberTabView: View {
    @Binding var selection: MemberTab
    @Binding var bookingSegment: BookingSegment
    @Binding var sheet: MemberSheet?
    let openMine: () -> Void
    let openBooking: (BookingSegment) -> Void

    var body: some View {
        TabView(selection: $selection) {
            Tab("首页", systemImage: MemberTabIcon.home, value: MemberTab.home) {
                HomeView(sheet: $sheet, openMine: openMine, openBooking: openBooking)
            }

            Tab("预约", systemImage: MemberTabIcon.booking, value: MemberTab.booking) {
                BookingView(sheet: $sheet, segment: $bookingSegment)
            }

            Tab("签到", systemImage: MemberTabIcon.checkin, value: MemberTab.checkin) {
                CheckinSheet(mode: .code, showsClose: false)
            }

            Tab("日程", systemImage: MemberTabIcon.records, value: MemberTab.records) {
                BookingsSheet(showsClose: false)
            }

            Tab("搜索", systemImage: MemberTabIcon.search, value: MemberTab.search, role: .search) {
                SearchSheet(sheet: $sheet, showsClose: false, openBooking: openBooking)
            }
        }
    }
}

private struct LegacyMemberTabView: View {
    @Binding var selection: MemberTab
    @Binding var bookingSegment: BookingSegment
    @Binding var sheet: MemberSheet?
    let openMine: () -> Void
    let openBooking: (BookingSegment) -> Void

    var body: some View {
        TabView(selection: $selection) {
            HomeView(sheet: $sheet, openMine: openMine, openBooking: openBooking)
                .tabItem { Label("首页", systemImage: MemberTabIcon.home) }
                .tag(MemberTab.home)

            BookingView(sheet: $sheet, segment: $bookingSegment)
                .tabItem { Label("预约", systemImage: MemberTabIcon.booking) }
                .tag(MemberTab.booking)

            CheckinSheet(mode: .code, showsClose: false)
                .tabItem { Label("签到", systemImage: MemberTabIcon.checkin) }
                .tag(MemberTab.checkin)

            BookingsSheet(showsClose: false)
                .tabItem { Label("日程", systemImage: MemberTabIcon.records) }
                .tag(MemberTab.records)

            SearchSheet(sheet: $sheet, showsClose: false, openBooking: openBooking)
                .tabItem { Label("搜索", systemImage: MemberTabIcon.search) }
                .tag(MemberTab.search)
        }
    }
}

private enum CoachTab: Hashable {
    case today
    case schedule
    case approvals
    case logs
}

private struct CoachShell: View {
    @State private var selectedTab: CoachTab = .today

    var body: some View {
        TabView(selection: $selectedTab) {
            CoachTodayView()
                .tabItem { Label("工作台", systemImage: "rectangle.grid.2x2") }
                .tag(CoachTab.today)

            CoachScheduleView()
                .tabItem { Label("课程表", systemImage: "calendar") }
                .tag(CoachTab.schedule)

            CoachApprovalsView()
                .tabItem { Label("确认", systemImage: "checkmark.seal") }
                .tag(CoachTab.approvals)

            CoachTrainingLogsView()
                .tabItem { Label("训练记录", systemImage: "square.and.pencil") }
                .tag(CoachTab.logs)
        }
        .tint(Color(red: 0.02, green: 0.75, blue: 0.66))
    }
}

private struct CoachTodayView: View {
    @EnvironmentObject private var store: MemberStore

    private var todaySchedule: [CoachScheduleItem] {
        store.coachSchedules.filter { DateTools.isToday($0.startTime) }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    CoachHeroCard()

                    CoachMetricPanel(dashboard: store.coachDashboard)

                    CoachSection(title: "今日课程", more: nil) {
                        if todaySchedule.isEmpty {
                            EmptyText("今天暂无排课")
                        } else {
                            ForEach(todaySchedule.prefix(3)) { item in
                                CoachScheduleRow(item: item)
                                if item.id != todaySchedule.prefix(3).last?.id {
                                    Divider().overlay(Palette.line)
                                }
                            }
                        }
                    }

                    CoachSection(title: "待确认预约", more: nil) {
                        if store.pendingCoachBookings.isEmpty {
                            EmptyText("暂无待确认预约")
                        } else {
                            ForEach(store.pendingCoachBookings.prefix(3)) { booking in
                                CoachBookingApprovalRow(booking: booking, compact: true)
                                if booking.id != store.pendingCoachBookings.prefix(3).last?.id {
                                    Divider().overlay(Palette.line)
                                }
                            }
                        }
                    }

                    CoachSection(title: "最近训练记录", more: nil) {
                        if store.trainingLogs.isEmpty {
                            EmptyText("暂无训练记录")
                        } else {
                            ForEach(store.trainingLogs.prefix(3)) { log in
                                TrainingLogRow(log: log)
                                if log.id != store.trainingLogs.prefix(3).last?.id {
                                    Divider().overlay(Palette.line)
                                }
                            }
                        }
                    }
                }
                .padding(16)
                .padding(.bottom, 86)
            }
            .background(Palette.page)
            .navigationTitle("教练工作台")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("退出") { store.logout() }
                }
            }
            .refreshable { await store.refreshAll() }
        }
    }
}

private struct CoachHeroCard: View {
    @EnvironmentObject private var store: MemberStore

    private var avatarURL: URL? {
        URL(string: store.session?.avatar?.nonEmpty ?? "") ?? OriginalAssets.coachAvatar(name: store.session?.displayName, index: 0)
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: [Palette.main.opacity(0.92), .white, Palette.orange.opacity(0.18)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            Circle()
                .fill(.white.opacity(0.38))
                .frame(width: 150, height: 150)
                .offset(x: 210, y: -50)
            Capsule()
                .fill(Color(red: 0.08, green: 0.74, blue: 0.70).opacity(0.28))
                .frame(width: 280, height: 42)
                .rotationEffect(.degrees(-14))
                .offset(x: -50, y: -70)

            HStack(spacing: 14) {
                CoachPhoto(name: store.session?.displayName ?? "教练", url: avatarURL, size: 74)
                    .overlay(Circle().stroke(.white.opacity(0.85), lineWidth: 2))

                VStack(alignment: .leading, spacing: 8) {
                    Text(store.session?.displayName ?? "教练")
                        .font(.system(size: 29, weight: .black, design: .rounded))
                        .lineLimit(1)
                    Text(store.session?.phone?.nonEmpty ?? "教练工作端")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Palette.text.opacity(0.62))
                    HStack(spacing: 8) {
                        Text(store.session?.roleLabel ?? "教练")
                            .font(.caption.weight(.black))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Palette.text.opacity(0.86), in: Capsule())
                        Text("Coach Workspace")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Palette.text.opacity(0.60))
                    }
                }
                Spacer()
            }
            .padding(18)
        }
        .frame(height: 150)
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
    }
}

private struct CoachMetricPanel: View {
    let dashboard: CoachDashboard?

    var body: some View {
        HStack(spacing: 0) {
            CoachMetricItem(label: "今日课", value: "\(dashboard?.todayLessons ?? 0)")
            Divider().frame(height: 34).overlay(Palette.line)
            CoachMetricItem(label: "待确认", value: "\(dashboard?.pendingApprovals ?? 0)")
            Divider().frame(height: 34).overlay(Palette.line)
            CoachMetricItem(label: "学员", value: "\(dashboard?.totalStudents ?? 0)")
            Divider().frame(height: 34).overlay(Palette.line)
            CoachMetricItem(label: "签到", value: "\(dashboard?.todayCheckins ?? 0)")
        }
        .padding(.vertical, 14)
        .appCard(radius: 18)
    }
}

private struct CoachMetricItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Palette.muted)
            Text(value)
                .font(.headline.weight(.black))
                .foregroundStyle(Palette.text)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct CoachScheduleView: View {
    @EnvironmentObject private var store: MemberStore
    @State private var weekStart = DateTools.startOfWeek(Date())

    private var weekDates: [Date] {
        DateTools.weekDates(from: weekStart)
    }

    private var weekItems: [CoachScheduleItem] {
        store.coachSchedules.filter { item in
            weekDates.contains { DateTools.isSameDay(item.startTime, as: $0) }
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                CoachScheduleTopBar(
                    weekStart: weekStart,
                    previousWeek: { weekStart = DateTools.addWeeks(-1, to: weekStart) },
                    currentWeek: { weekStart = DateTools.startOfWeek(Date()) },
                    nextWeek: { weekStart = DateTools.addWeeks(1, to: weekStart) }
                )
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 10)

                CoachWeekDayHeader(weekDates: weekDates)

                Divider().overlay(Palette.line)

                ScrollView(.vertical, showsIndicators: false) {
                    CoachWeekGrid(weekDates: weekDates, items: weekItems)
                        .padding(.horizontal, 10)
                        .padding(.bottom, 94)
                }
            }
            .background(Palette.page)
            .toolbar(.hidden, for: .navigationBar)
            .refreshable { await store.refreshAll() }
        }
    }
}

private struct CoachScheduleTopBar: View {
    let weekStart: Date
    let previousWeek: () -> Void
    let currentWeek: () -> Void
    let nextWeek: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "line.3.horizontal")
                .font(.system(size: 22, weight: .medium))
                .frame(width: 34, height: 34)

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text("第 \(DateTools.weekNumber(weekStart)) 周")
                        .font(.title3.weight(.black))
                        .foregroundStyle(Palette.orange)
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Palette.text.opacity(0.62))
                }
                Text(DateTools.weekRangeTitle(weekStart))
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Palette.muted)
            }

            Spacer()

            HStack(spacing: 8) {
                Button(action: previousWeek) {
                    Image(systemName: "chevron.left")
                }
                Button(action: currentWeek) {
                    Text("本周")
                        .font(.caption.weight(.bold))
                }
                Button(action: nextWeek) {
                    Image(systemName: "chevron.right")
                }
            }
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(Palette.text)

            Button(action: {}) {
                Image(systemName: "plus")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 34, height: 34)
                    .background(LinearGradient(colors: [Color(red: 0.22, green: 0.46, blue: 1), Color(red: 0.12, green: 0.78, blue: 0.88)], startPoint: .topLeading, endPoint: .bottomTrailing), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .buttonStyle(.plain)
    }
}

private struct CoachWeekDayHeader: View {
    let weekDates: [Date]

    var body: some View {
        HStack(spacing: 0) {
            Text(DateTools.monthLabel(weekDates.first ?? Date()))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Palette.text.opacity(0.72))
                .frame(width: 52)
            ForEach(weekDates, id: \.self) { date in
                VStack(spacing: 5) {
                    Text(DateTools.weekdaySymbol(date))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Calendar.current.isDateInToday(date) ? Palette.orange : Palette.text)
                    Text(DateTools.dayNumber(date))
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Calendar.current.isDateInToday(date) ? Palette.orange : Palette.muted)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 10)
        .padding(.bottom, 10)
        .background(Palette.page)
    }
}

private struct CoachWeekGrid: View {
    let weekDates: [Date]
    let items: [CoachScheduleItem]

    private let startHour = 6
    private let endHour = 22
    private let hourHeight: CGFloat = 78
    private let leftWidth: CGFloat = 42

    private var totalHours: Int {
        endHour - startHour
    }

    private var gridHeight: CGFloat {
        CGFloat(totalHours) * hourHeight
    }

    var body: some View {
        GeometryReader { proxy in
            let columnWidth = max((proxy.size.width - leftWidth) / 7, 36)

            ZStack(alignment: .topLeading) {
                CoachGridLines(
                    totalHours: totalHours,
                    hourHeight: hourHeight,
                    leftWidth: leftWidth,
                    columnWidth: columnWidth
                )

                ForEach(startHour..<endHour, id: \.self) { hour in
                    VStack(alignment: .trailing, spacing: 3) {
                        Text("\(hour)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Palette.text.opacity(0.60))
                        Text(String(format: "%02d:00", hour))
                            .font(.caption2)
                            .foregroundStyle(Palette.muted)
                    }
                    .frame(width: leftWidth - 9, height: hourHeight, alignment: .topTrailing)
                    .offset(x: 0, y: CGFloat(hour - startHour) * hourHeight + 9)
                }

                ForEach(items) { item in
                    if let frame = eventFrame(for: item, columnWidth: columnWidth) {
                        CoachTimetableEvent(item: item)
                            .frame(width: max(columnWidth - 8, 28), height: frame.height)
                            .offset(x: frame.x, y: frame.y)
                    }
                }

                if items.isEmpty {
                    Text("本周暂无课程")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Palette.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 42)
                        .offset(x: leftWidth / 2)
                }
            }
            .frame(width: proxy.size.width, height: gridHeight)
        }
        .frame(height: gridHeight)
    }

    private func eventFrame(for item: CoachScheduleItem, columnWidth: CGFloat) -> (x: CGFloat, y: CGFloat, height: CGFloat)? {
        guard let start = DateTools.parse(item.startTime),
              let end = DateTools.parse(item.endTime),
              let dayIndex = weekDates.firstIndex(where: { Calendar.current.isDate(start, inSameDayAs: $0) }) else {
            return nil
        }

        let startMinutes = Calendar.current.component(.hour, from: start) * 60 + Calendar.current.component(.minute, from: start)
        let endMinutes = Calendar.current.component(.hour, from: end) * 60 + Calendar.current.component(.minute, from: end)
        let gridStart = startHour * 60
        let gridEnd = endHour * 60
        let clampedStart = min(max(startMinutes, gridStart), gridEnd)
        let clampedEnd = min(max(endMinutes, gridStart + 20), gridEnd)
        let duration = max(clampedEnd - clampedStart, 35)

        return (
            x: leftWidth + CGFloat(dayIndex) * columnWidth + 4,
            y: CGFloat(clampedStart - gridStart) / 60 * hourHeight + 4,
            height: CGFloat(duration) / 60 * hourHeight - 8
        )
    }
}

private struct CoachGridLines: View {
    let totalHours: Int
    let hourHeight: CGFloat
    let leftWidth: CGFloat
    let columnWidth: CGFloat

    var body: some View {
        ZStack(alignment: .topLeading) {
            ForEach(0...totalHours, id: \.self) { index in
                Rectangle()
                    .fill(Palette.line.opacity(0.86))
                    .frame(height: 1)
                    .offset(x: leftWidth, y: CGFloat(index) * hourHeight)
            }

            ForEach(0...7, id: \.self) { index in
                Rectangle()
                    .fill(Palette.line.opacity(index == 0 ? 0.0 : 0.34))
                    .frame(width: 1)
                    .offset(x: leftWidth + CGFloat(index) * columnWidth, y: 0)
            }
        }
    }
}

private struct CoachTimetableEvent: View {
    let item: CoachScheduleItem

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Capsule()
                .fill(item.accentColor.opacity(0.46))
                .frame(height: 7)
                .padding(.horizontal, -8)
                .padding(.top, -8)

            Text(item.shortName)
                .font(.caption.weight(.black))
                .foregroundStyle(item.accentColor)
                .lineLimit(4)
                .minimumScaleFactor(0.72)
            Text(item.venueName ?? "训练区")
                .font(.caption2.weight(.medium))
                .foregroundStyle(item.accentColor.opacity(0.78))
                .lineLimit(2)
                .minimumScaleFactor(0.72)
        }
        .padding(8)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(item.tintColor, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct CoachScheduleCard: View {
    let item: CoachScheduleItem

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            CoachScheduleRow(item: item)
            HStack(spacing: 8) {
                ScheduleChip(text: item.typeText, icon: item.iconName)
                ScheduleChip(text: item.statusText, icon: "circle.fill")
                Spacer()
                Text(item.capacityText)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.muted)
            }
            if let description = item.description?.nonEmpty {
                Text(description)
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                    .lineLimit(2)
            }
        }
        .padding(14)
        .appCard(radius: 18)
    }
}

private struct CoachScheduleRow: View {
    let item: CoachScheduleItem

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 15, style: .continuous)
                    .fill(item.isPrivate ? Palette.orange.opacity(0.16) : Palette.mainSoft)
                Image(systemName: item.iconName)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(item.isPrivate ? Palette.orange : Palette.text)
            }
            .frame(width: 48, height: 48)

            VStack(alignment: .leading, spacing: 5) {
                Text(item.name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Palette.text)
                    .lineLimit(1)
                Text("\(DateTools.range(item.startTime, item.endTime)) · \(item.venueName ?? "训练区")")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                    .lineLimit(1)
            }
            Spacer()
        }
    }
}

private struct ScheduleChip: View {
    let text: String
    let icon: String

    var body: some View {
        Label(text, systemImage: icon)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Palette.text.opacity(0.72))
            .padding(.horizontal, 9)
            .padding(.vertical, 6)
            .background(Palette.mainSoft, in: Capsule())
    }
}

private struct CoachApprovalsView: View {
    @EnvironmentObject private var store: MemberStore

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 12) {
                    if store.pendingCoachBookings.isEmpty {
                        EmptyBlock(text: "暂无待确认预约")
                    } else {
                        ForEach(store.pendingCoachBookings) { booking in
                            CoachBookingApprovalRow(booking: booking, compact: false)
                                .padding(14)
                                .appCard(radius: 18)
                        }
                    }
                }
                .padding(16)
                .padding(.bottom, 86)
            }
            .background(Palette.page)
            .navigationTitle("学员预约确认")
            .refreshable { await store.refreshAll() }
        }
    }
}

private struct CoachBookingApprovalRow: View {
    @EnvironmentObject private var store: MemberStore
    let booking: BookingOrder
    let compact: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 11) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "person.crop.circle.badge.checkmark")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(Palette.text)
                    .frame(width: 46, height: 46)
                    .background(Palette.mainSoft, in: RoundedRectangle(cornerRadius: 15, style: .continuous))

                VStack(alignment: .leading, spacing: 5) {
                    Text(booking.resourceName ?? "私教预约")
                        .font(.headline.weight(.bold))
                        .lineLimit(1)
                    Text("会员ID \(booking.userId.map(String.init) ?? "-") · \(DateTools.range(booking.startTime, booking.endTime))")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                        .lineLimit(2)
                }
                Spacer()
                TagText(text: booking.statusText, color: booking.statusColor)
            }

            if !compact {
                Text("系统只返回当前教练自己的预约，确认后会员端即可生成签到码。")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
            }

            HStack(spacing: 10) {
                Button("确认") {
                    Task {
                        await store.perform {
                            try await store.reviewCoachBooking(booking, approved: true)
                            return "已确认预约"
                        }
                    }
                }
                .buttonStyle(SmallGreenButtonStyle())

                Button("拒绝") {
                    Task {
                        await store.perform {
                            try await store.reviewCoachBooking(booking, approved: false)
                            return "已拒绝预约"
                        }
                    }
                }
                .buttonStyle(SmallOrangeButtonStyle())
            }
        }
        .padding(.vertical, compact ? 10 : 0)
    }
}

private struct CoachTrainingLogsView: View {
    @EnvironmentObject private var store: MemberStore
    @State private var showForm = false

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 12) {
                    if store.trainingLogs.isEmpty {
                        EmptyBlock(text: "暂无训练记录")
                    } else {
                        ForEach(store.trainingLogs) { log in
                            TrainingLogRow(log: log)
                                .padding(14)
                                .appCard(radius: 18)
                        }
                    }
                }
                .padding(16)
                .padding(.bottom, 86)
            }
            .background(Palette.page)
            .navigationTitle("训练记录")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showForm = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showForm) {
                TrainingLogFormSheet()
            }
            .refreshable { await store.refreshAll() }
        }
    }
}

private struct TrainingLogRow: View {
    let log: TrainingLog

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "figure.strengthtraining.traditional")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Palette.text)
                .frame(width: 46, height: 46)
                .background(Palette.mainSoft, in: RoundedRectangle(cornerRadius: 15, style: .continuous))

            VStack(alignment: .leading, spacing: 6) {
                Text(log.focusArea?.nonEmpty ?? "训练记录")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Palette.text)
                    .lineLimit(1)
                Text("会员ID \(log.userId.map(String.init) ?? "-") · \(DateTools.displayDate(log.trainDate))")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                Text(log.content?.nonEmpty ?? log.remark?.nonEmpty ?? "暂无内容")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                    .lineLimit(2)
            }
            Spacer()
        }
        .padding(.vertical, 10)
    }
}

private struct TrainingLogFormSheet: View {
    @EnvironmentObject private var store: MemberStore
    @Environment(\.dismiss) private var dismiss
    @State private var userId = ""
    @State private var bookingId = ""
    @State private var trainDate = Date()
    @State private var focusArea = ""
    @State private var content = ""
    @State private var remark = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    FieldRow(title: "会员ID", text: $userId, icon: "person")
                        .keyboardType(.numberPad)
                    FieldRow(title: "预约单ID（可选）", text: $bookingId, icon: "calendar")
                        .keyboardType(.numberPad)
                    DatePicker("训练日期", selection: $trainDate, displayedComponents: .date)
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 14)
                        .frame(height: 48)
                        .background(.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    FieldRow(title: "训练重点", text: $focusArea, icon: "target")
                    TextEditor(text: $content)
                        .frame(minHeight: 120)
                        .padding(10)
                        .scrollContentBackground(.hidden)
                        .background(.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .overlay(alignment: .topLeading) {
                            if content.isEmpty {
                                Text("训练内容")
                                    .font(.body)
                                    .foregroundStyle(Palette.muted)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 18)
                                    .allowsHitTesting(false)
                            }
                        }
                    FieldRow(title: "备注（可选）", text: $remark, icon: "text.bubble")

                    Button("保存训练记录") {
                        Task {
                            await store.perform {
                                try await store.saveTrainingLog(
                                    userIdText: userId,
                                    bookingIdText: bookingId,
                                    trainDate: trainDate,
                                    focusArea: focusArea,
                                    content: content,
                                    remark: remark
                                )
                                dismiss()
                                return "训练记录已保存"
                            }
                        }
                    }
                    .buttonStyle(MainButtonStyle())
                }
                .padding(16)
            }
            .background(Palette.page)
            .navigationTitle("填写训练记录")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }
}

private struct CoachSection<Content: View>: View {
    let title: String
    let more: String?
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                SectionTitle(title)
                Spacer()
                if let more {
                    Text(more)
                        .font(.caption.weight(.medium))
                        .foregroundStyle(Palette.muted)
                }
            }
            VStack(spacing: 0) {
                content
            }
        }
        .padding(14)
        .appCard(radius: 18)
    }
}

private struct LoginView: View {
    @EnvironmentObject private var store: MemberStore
    @State private var username = "member01"
    @State private var password = "123456"
    @State private var baseURL = "http://127.0.0.1:9090"
    @State private var showsPassword = false

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 22) {
                LoginHero()
                loginPanel
            }
            .padding(.horizontal, 20)
            .padding(.top, 26)
            .padding(.bottom, 34)
        }
        .background(
            LinearGradient(
                colors: [Palette.main.opacity(0.16), Palette.page, Palette.page],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .onAppear {
            baseURL = store.baseURLString
        }
    }

    private var loginPanel: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                LoginIconBadge(icon: "key.fill", tint: Palette.main)
                VStack(alignment: .leading, spacing: 3) {
                    Text("账号登录")
                        .font(.headline.weight(.bold))
                    Text("会员和教练共用入口")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
            }

            VStack(spacing: 12) {
                LoginFieldRow(title: "后端地址", text: $baseURL, icon: "server.rack", keyboardType: .URL)
                LoginFieldRow(title: "账号", text: $username, icon: "person.crop.circle")
                LoginSecureFieldRow(title: "密码", text: $password, icon: "lock.shield", showsPassword: $showsPassword)
            }

            Button {
                Task {
                    await store.perform {
                        try await store.login(username: username, password: password, baseURL: baseURL)
                        return "登录成功"
                    }
                }
            } label: {
                HStack(spacing: 8) {
                    Text("登录")
                    Image(systemName: "arrow.right")
                        .font(.system(size: 15, weight: .bold))
                }
            }
            .buttonStyle(LoginButtonStyle())

            VStack(alignment: .leading, spacing: 10) {
                Text("演示账号")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.muted)
                HStack(spacing: 10) {
                    LoginDemoAccountButton(role: "会员", account: "member01", icon: "person.crop.circle") {
                        username = "member01"
                        password = "123456"
                    }
                    LoginDemoAccountButton(role: "教练", account: "coach01", icon: "figure.strengthtraining.traditional") {
                        username = "coach01"
                        password = "123456"
                    }
                }
            }
            .padding(.top, 2)
        }
        .padding(18)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(Palette.line, lineWidth: 1))
    }
}

private struct LoginHero: View {
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: [Palette.main.opacity(0.92), .white, Palette.orange.opacity(0.16)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            CachedRemoteImage(url: OriginalAssets.homeBanner) {
                Color.clear
            }
            .opacity(0.18)
            .saturation(0.9)

            VStack(alignment: .leading, spacing: 18) {
                HStack(alignment: .center, spacing: 13) {
                    CachedRemoteImage(url: OriginalAssets.storeLogo) {
                        Image(systemName: "figure.strengthtraining.traditional")
                            .font(.system(size: 25, weight: .bold))
                            .foregroundStyle(Palette.text.opacity(0.62))
                    }
                    .frame(width: 58, height: 58)
                    .background(.white.opacity(0.55), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(.white.opacity(0.78), lineWidth: 1))

                    VStack(alignment: .leading, spacing: 4) {
                        Text("BOOMGYM")
                            .font(.system(size: 35, weight: .black, design: .rounded))
                        Text("健身房会员预约系统")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Palette.text.opacity(0.72))
                    }
                }

                HStack(spacing: 8) {
                    LoginHeroBadge(text: "团课预约")
                    LoginHeroBadge(text: "私教课程")
                    LoginHeroBadge(text: "签到入场")
                }
            }
            .padding(22)
        }
        .frame(height: 214)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 28, style: .continuous).stroke(.white.opacity(0.72), lineWidth: 1))
    }
}

private struct LoginHeroBadge: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundStyle(Palette.text)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(.white.opacity(0.58), in: Capsule())
    }
}

private struct LoginFieldRow: View {
    let title: String
    @Binding var text: String
    let icon: String
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        HStack(spacing: 12) {
            LoginIconBadge(icon: icon, tint: Palette.main)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(Palette.muted)
                TextField(title, text: $text)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Palette.text)
                    .keyboardType(keyboardType)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
        }
        .padding(.horizontal, 13)
        .padding(.vertical, 11)
        .background(Color(red: 0.98, green: 0.985, blue: 0.975), in: RoundedRectangle(cornerRadius: 15, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 15, style: .continuous).stroke(Palette.main.opacity(0.20), lineWidth: 1))
    }
}

private struct LoginSecureFieldRow: View {
    let title: String
    @Binding var text: String
    let icon: String
    @Binding var showsPassword: Bool

    var body: some View {
        HStack(spacing: 12) {
            LoginIconBadge(icon: icon, tint: Palette.orange)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(Palette.muted)

                Group {
                    if showsPassword {
                        TextField(title, text: $text)
                    } else {
                        SecureField(title, text: $text)
                    }
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Palette.text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            }

            Button {
                showsPassword.toggle()
            } label: {
                Image(systemName: showsPassword ? "eye.slash" : "eye")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Palette.muted)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 13)
        .padding(.vertical, 11)
        .background(Color(red: 0.98, green: 0.985, blue: 0.975), in: RoundedRectangle(cornerRadius: 15, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 15, style: .continuous).stroke(Palette.orange.opacity(0.18), lineWidth: 1))
    }
}

private struct LoginIconBadge: View {
    let icon: String
    let tint: Color

    var body: some View {
        RoundedRectangle(cornerRadius: 12, style: .continuous)
            .fill(tint.opacity(0.16))
            .frame(width: 38, height: 38)
            .overlay {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(tint)
            }
    }
}

private struct LoginDemoAccountButton: View {
    let role: String
    let account: String
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Palette.text)
                VStack(alignment: .leading, spacing: 2) {
                    Text(role)
                        .font(.caption.weight(.bold))
                    Text(account)
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(Palette.muted)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            .background(Palette.mainSoft, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct LoginButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline.weight(.bold))
            .foregroundStyle(Palette.text)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(Palette.main.opacity(configuration.isPressed ? 0.72 : 1), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct HomeView: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var store: MemberStore
    @Binding var sheet: MemberSheet?
    let openMine: () -> Void
    let openBooking: (BookingSegment) -> Void

    var storeInfo: Venue? { store.venues.first }
    var avatarURL: URL? { URL(string: store.session?.avatar?.nonEmpty ?? "") ?? OriginalAssets.memberAvatar(index: 0) }
    var storeAddress: String {
        guard let location = storeInfo?.location?.nonEmpty else {
            return homeGymAddress
        }
        return "\(homeGymAddress) · \(location)"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                VStack(spacing: 10) {
                    HomeHero(
                        storeName: homeGymDisplayName,
                        venue: storeInfo
                    )
                    .frame(height: 148)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                    HomeStoreInfoPanel(
                        storeName: homeGymNavName,
                        address: storeAddress,
                        openDetail: { openBooking(.venue) },
                        callAction: { store.alert = "暂未配置门店电话" }
                    )
                }
                .padding(.horizontal, 18)
                .padding(.top, 10)

                VStack(spacing: 14) {
                        AssetPanel(home: store.home)

                        BoxView(title: "会员套餐", more: "查看资产", action: { sheet = .wallet }) {
                        VStack(spacing: 0) {
                            if store.membershipPackages.isEmpty {
                                EmptyText("暂无可售会籍")
                            } else {
                                ForEach(Array(store.membershipPackages.prefix(3).enumerated()), id: \.element.id) { index, item in
                                    PackageLine(
                                        title: item.name,
                                        subtitle: "\(item.days ?? 0) 天有效期",
                                        price: item.price,
                                        coverURL: OriginalAssets.packageCover(index: index)
                                    ) {
                                        Task {
                                            await store.perform {
                                                try await store.purchase(.membership(item))
                                                return "购买成功"
                                            }
                                        }
                                    }
                                    if item.id != store.membershipPackages.prefix(3).last?.id {
                                        Divider().overlay(Palette.line)
                                    }
                                }
                            }
                        }
                    }

                    BoxView(title: "可约团课", more: "去预约", action: { openBooking(.course) }) {
                        VStack(spacing: 0) {
                            if store.courses.isEmpty {
                                EmptyText("暂无可约团课")
                            } else {
                                ForEach(store.courses.prefix(4)) { item in
                                    CourseCompactRow(course: item)
                                    if item.id != store.courses.prefix(4).last?.id {
                                        Divider().overlay(Palette.line)
                                    }
                                }
                            }
                        }
                    }

                    BoxView(title: "明星教练", more: "预约私教", action: { openBooking(.coach) }) {
                        if store.coaches.isEmpty {
                            EmptyText("暂无可约教练")
                        } else {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 14) {
                                    ForEach(Array(store.coaches.prefix(4).enumerated()), id: \.element.id) { index, coach in
                                        CoachTile(coach: coach, index: index)
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                        }
                    }

                    BoxView(title: "最近预约", more: "查看记录", action: { sheet = .bookings }) {
                        VStack(spacing: 0) {
                            if store.upcomingBookings.isEmpty {
                                EmptyText("暂无待参加预约")
                            } else {
                                ForEach(store.upcomingBookings.prefix(4)) { item in
                                    BookingLine(booking: item)
                                    if item.id != store.upcomingBookings.prefix(4).last?.id {
                                        Divider().overlay(Palette.line)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 104)
            }
        }
        .refreshable { await store.refreshAll() }
        .overlay(alignment: .top) {
            HomeHeroControlBar(
                sheet: $sheet,
                storeName: homeGymNavName,
                avatarURL: avatarURL,
                openMine: openMine,
                colorScheme: colorScheme
            )
            .padding(.horizontal, 30)
            .padding(.top, 22)
            .zIndex(10)
        }
        .background(Palette.page)
    }
}

private struct HomeNav: View {
    @Binding var sheet: MemberSheet?
    let storeName: String

    var body: some View {
        HStack(spacing: 8) {
            Button {
                sheet = .checkin
            } label: {
                Label("入场凭证", systemImage: "qrcode")
            }

            Button {
                sheet = .bookings
            } label: {
                Label(storeName, systemImage: "location.fill")
                    .lineLimit(1)
            }
        }
        .font(.caption.weight(.medium))
        .foregroundStyle(Palette.text)
        .padding(6)
        .background(Color.white.opacity(0.90), in: Capsule())
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct HomeHero: View {
    let storeName: String
    let venue: Venue?

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            VenueCover(venue: venue)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .scaleEffect(1.02)
                .clipped()

            LinearGradient(
                colors: [.black.opacity(0.02), .black.opacity(0.16), .black.opacity(0.64)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 5) {
                Spacer()
                Text(storeName)
                    .font(.system(size: 25, weight: .black, design: .rounded))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.74)
                    .shadow(color: .black.opacity(0.20), radius: 8, x: 0, y: 4)
                Text(venue?.description?.nonEmpty ?? "自由力量与综合训练区")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.86))
                    .lineLimit(1)
            }
            .padding(.horizontal, 15)
            .padding(.bottom, 13)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        }
    }
}

private struct HomeStoreInfoPanel: View {
    let storeName: String
    let address: String
    let openDetail: () -> Void
    let callAction: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Button(action: openDetail) {
                HStack(alignment: .center, spacing: 11) {
                    CachedRemoteImage(url: OriginalAssets.storeLogo, contentMode: .fill) {
                        Circle()
                            .fill(Palette.text)
                            .overlay(
                                Text("BOOM")
                                    .font(.system(size: 9, weight: .black))
                                    .foregroundStyle(.white)
                            )
                    }
                    .frame(width: 46, height: 46)
                    .clipShape(Circle())

                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 6) {
                            Text(storeName)
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(Palette.text)
                                .lineLimit(1)
                                .minimumScaleFactor(0.82)
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(Palette.muted)
                        }

                        HStack(alignment: .top, spacing: 6) {
                            Image(systemName: "mappin.and.ellipse")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(Palette.muted.opacity(0.62))
                                .padding(.top, 1)
                            Text(address)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(Palette.muted)
                                .lineLimit(2)
                                .lineSpacing(1)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .buttonStyle(.plain)

            Button(action: callAction) {
                VStack(spacing: 3) {
                    Image(systemName: "phone")
                        .font(.system(size: 18, weight: .medium))
                    Text("电话")
                        .font(.system(size: 10, weight: .medium))
                }
                .foregroundStyle(Palette.main.opacity(0.88))
                .frame(width: 36)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 13)
        .padding(.vertical, 10)
        .background(Palette.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct HomeQuickActions: View {
    @Binding var sheet: MemberSheet?
    let openBooking: (BookingSegment) -> Void

    var body: some View {
        HStack(spacing: 0) {
            HomeModuleButton(title: "团课", icon: "figure.run") { openBooking(.course) }
            HomeModuleButton(title: "私教", icon: "figure.strengthtraining.traditional") { openBooking(.coach) }
            HomeModuleButton(title: "场馆", icon: "building.2") { openBooking(.venue) }
            HomeModuleButton(title: "签到", icon: "qrcode.viewfinder") { sheet = .checkin }
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 5)
        .homeGlassCapsule(fill: .white.opacity(0.70), stroke: .white.opacity(0.78))
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct HomeHeroControlBar: View {
    @Binding var sheet: MemberSheet?
    let storeName: String
    let avatarURL: URL?
    let openMine: () -> Void
    let colorScheme: ColorScheme

    private var usesDarkContent: Bool {
        colorScheme == .light
    }

    private var foreground: Color {
        usesDarkContent ? .black : .white
    }

    private var controlFill: Color {
        usesDarkContent ? .white.opacity(0.12) : .black.opacity(0.18)
    }

    private var controlStroke: Color {
        usesDarkContent ? .white.opacity(0.96) : .white.opacity(0.44)
    }

    private var avatarStroke: Color {
        usesDarkContent ? .white.opacity(0.92) : .white.opacity(0.72)
    }

    var body: some View {
        Group {
            if #available(iOS 26.0, *) {
                GlassEffectContainer(spacing: 8) {
                    controls
                }
            } else {
                controls
            }
        }
        .frame(maxWidth: .infinity)
        .animation(.easeInOut(duration: 0.18), value: usesDarkContent)
    }

    private var controls: some View {
        HStack(spacing: 8) {
            Button {
                sheet = .checkin
            } label: {
                Image(systemName: "qrcode.viewfinder")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(foreground)
                    .frame(width: 46, height: 34)
                    .homeGlassCapsule(fill: controlFill, stroke: controlStroke)
            }
            .accessibilityLabel("入场凭证")
            .buttonStyle(.plain)

            Button {
                sheet = .bookings
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: "location.fill")
                        .font(.system(size: 12, weight: .bold))
                    Text(storeName)
                        .font(.system(size: 12, weight: .bold))
                        .lineLimit(1)
                }
                .foregroundStyle(foreground)
                .frame(height: 34)
                .padding(.horizontal, 12)
                .fixedSize(horizontal: true, vertical: false)
                .homeGlassCapsule(fill: controlFill, stroke: controlStroke)
            }
            .buttonStyle(.plain)

            Spacer(minLength: 8)

            HStack(spacing: 8) {
                Button {
                    openMine()
                } label: {
                    CachedRemoteImage(url: avatarURL) {
                        Circle()
                            .fill(controlFill)
                            .overlay(Image(systemName: "person.fill").font(.caption.weight(.bold)).foregroundStyle(foreground))
                    }
                    .frame(width: 32, height: 32)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(avatarStroke, lineWidth: 1.5))
                    .shadow(color: .black.opacity(usesDarkContent ? 0.06 : 0.16), radius: 6, x: 0, y: 3)
                }
                .buttonStyle(.plain)

                Menu {
                    Button("钱包资产") { sheet = .wallet }
                    Button("个人日程") { sheet = .bookings }
                    Button("入场凭证") { sheet = .checkin }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 16, weight: .black))
                        .foregroundStyle(foreground)
                        .frame(width: 42, height: 34)
                        .homeGlassCapsule(fill: controlFill, stroke: controlStroke)
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

private struct FloatingSearchButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 23, weight: .bold))
                .foregroundStyle(Palette.text)
                .frame(width: 60, height: 60)
                .background(Color.white.opacity(0.92), in: Circle())
                .overlay(Circle().stroke(Palette.main.opacity(0.82), lineWidth: 2))
        }
        .buttonStyle(.plain)
    }
}

private struct HomeModuleButton: View {
    let title: String
    let icon: String
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Palette.orange)
                Text(title)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Palette.text)
            }
            .frame(height: 40)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

private struct GlassChipButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.caption.weight(.semibold))
            .foregroundStyle(Palette.text)
            .padding(.horizontal, 10)
            .frame(height: 36)
            .background(Color.white.opacity(0.42), in: Capsule())
            .opacity(configuration.isPressed ? 0.76 : 1)
    }
}

private struct RemoteBanner: View {
    var body: some View {
        CachedRemoteImage(url: OriginalAssets.homeBanner) {
            LinearGradient(colors: [Palette.main.opacity(0.95), .white], startPoint: .topLeading, endPoint: .bottomTrailing)
        }
    }
}

private struct LegacyRemoteBanner: View {
    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .bottomLeading) {
                LinearGradient(
                    colors: [Palette.main.opacity(0.95), .white, Palette.orange.opacity(0.24)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Circle()
                    .fill(.white.opacity(0.42))
                    .frame(width: 190, height: 190)
                    .offset(x: proxy.size.width * 0.55, y: -50)
                Circle()
                    .fill(Palette.orange.opacity(0.13))
                    .frame(width: 260, height: 260)
                    .offset(x: proxy.size.width * 0.38, y: 80)

                AsyncImage(url: OriginalAssets.storeLogo) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        Image(systemName: "figure.strengthtraining.traditional")
                            .resizable()
                            .scaledToFit()
                            .padding(38)
                            .foregroundStyle(Palette.text.opacity(0.55))
                            .background(.white.opacity(0.28))
                    }
                }
                .frame(width: 156, height: 156)
                .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 30, style: .continuous).stroke(.white.opacity(0.65), lineWidth: 2))
                .shadow(color: Palette.orange.opacity(0.22), radius: 18, x: 0, y: 10)
                .offset(x: proxy.size.width - 186, y: 34)

                VStack(alignment: .leading, spacing: 10) {
                    Text("MEMBER CENTER")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Palette.text.opacity(0.62))
                    Text("健身房场馆预约")
                        .font(.system(size: 31, weight: .black, design: .rounded))
                        .foregroundStyle(Palette.text)
                    HStack(spacing: 8) {
                        HeroPill(text: "团课")
                        HeroPill(text: "私教")
                        HeroPill(text: "签到")
                    }
                }
                .padding(24)
            }
        }
    }
}

private struct HeroPill: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundStyle(Palette.text)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(.white.opacity(0.58), in: Capsule())
    }
}

private struct StoreBoxView: View {
    let venue: Venue?
    let displayName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                VenueCover(venue: venue)
                    .frame(width: 92, height: 72)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

                VStack(alignment: .leading, spacing: 6) {
                    Text(displayName)
                        .font(.headline.weight(.semibold))
                        .lineLimit(2)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(Palette.muted)
            }
            .padding(12)
            .appCard(radius: 12)
        }
        .buttonStyle(.plain)
    }
}

private struct AssetPanel: View {
    let home: MemberHome?

    var body: some View {
        HStack(spacing: 0) {
            AssetItem(label: "余额", value: "¥\(money(home?.balance))")
            Divider().frame(height: 34).overlay(Palette.line)
            AssetItem(label: "会籍", value: home?.activeMembershipName?.nonEmpty ?? "未开通")
            Divider().frame(height: 34).overlay(Palette.line)
            AssetItem(label: "私教课", value: "\(home?.remainingPrivateSessions ?? 0) 节")
        }
        .padding(.vertical, 14)
        .appCard(radius: 12)
    }
}

private struct AssetItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 5) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Palette.muted)
            Text(value)
                .font(.headline.weight(.semibold))
                .foregroundStyle(Palette.text)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct BoxView<Content: View>: View {
    let title: String
    let more: String?
    let action: () -> Void
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 8) {
                    Rectangle()
                        .fill(Palette.main)
                        .frame(width: 4, height: 18)
                    Text(title)
                        .font(.title3.weight(.semibold))
                }
                Spacer()
                if let more {
                    Button(more, action: action)
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
            }
            content
        }
        .padding(14)
        .appCard(radius: 12)
    }
}

private struct CourseCompactRow: View {
    @EnvironmentObject private var store: MemberStore
    let course: CourseSlot

    var body: some View {
        HStack(spacing: 12) {
            CourseCover(
                label: course.flashSale == 1 ? "秒杀" : "团课",
                imageURL: OriginalAssets.courseAvatar(coachName: course.coachName, index: course.id)
            )
                .frame(width: 80, height: 70)

            VStack(alignment: .leading, spacing: 6) {
                Text(course.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text("\(course.coachName ?? "待排教练") | \(course.venueName ?? "训练区")")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                    .lineLimit(1)
                HStack {
                    Text("¥\(course.priceText)")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Palette.orange)
                    Text("余 \(course.availableCount ?? 0) 席")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
            }
            Spacer()
            Button("预约") {
                Task {
                    await store.perform {
                        try await store.bookCourse(course)
                    }
                }
            }
            .buttonStyle(SmallGreenButtonStyle())
            .disabled((course.availableCount ?? 0) <= 0)
        }
        .padding(.vertical, 8)
    }
}

private struct CoachTile: View {
    let coach: CoachProfile
    let index: Int

    var body: some View {
        VStack(spacing: 8) {
            CoachPhoto(name: coach.name, url: OriginalAssets.coachAvatar(name: coach.name, index: index), size: 70)
            Text(coach.name)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
            Text(coach.specialization ?? "私教课程")
                .font(.caption)
                .foregroundStyle(Palette.muted)
                .lineLimit(1)
        }
        .frame(width: 96)
    }
}

private struct BookingLine: View {
    let booking: BookingOrder

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 5) {
                Text(booking.resourceName ?? "预约")
                    .font(.subheadline.weight(.semibold))
                Text(DateTools.range(booking.startTime, booking.endTime))
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
            }
            Spacer()
            Text(booking.statusText)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Palette.orange)
        }
        .padding(.vertical, 10)
    }
}

private struct BookingView: View {
    @EnvironmentObject private var store: MemberStore
    @Binding var sheet: MemberSheet?
    @Binding var segment: BookingSegment
    @State private var selectedDate = DateTools.startOfDay(Date())

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                BookingTabBar(selection: $segment)
                .padding(.horizontal, 16)
                if segment != .course {
                    DateStrip(selection: $selectedDate)
                }

                switch segment {
                case .course:
                    CourseBookingList()
                case .coach:
                    CoachBookingList(selectedDate: selectedDate)
                case .venue:
                    VenueBookingList(selectedDate: selectedDate)
                }

                Button {
                    sheet = .bookings
                } label: {
                    Label("查看个人日程", systemImage: "calendar.badge.clock")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(OutlineButtonStyle())
                .padding(.horizontal, 16)
            }
            .padding(.top, 14)
            .padding(.bottom, 104)
        }
        .background(Palette.page)
        .refreshable { await store.refreshAll() }
    }
}

private struct BookingTabBar: View {
    @Binding var selection: BookingSegment

    var body: some View {
        HStack(spacing: 22) {
            ForEach(BookingSegment.allCases) { item in
                Button {
                    withAnimation(.spring(response: 0.28, dampingFraction: 0.82)) {
                        selection = item
                    }
                } label: {
                    VStack(spacing: 7) {
                        Text(item.title)
                            .font(.system(size: selection == item ? 21 : 17, weight: selection == item ? .bold : .medium))
                            .foregroundStyle(Palette.text)
                        Capsule()
                            .fill(selection == item ? Palette.main : .clear)
                            .frame(width: selection == item ? 28 : 10, height: 4)
                    }
                    .frame(minWidth: 58)
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
        .padding(.top, 4)
        .padding(.bottom, 2)
    }
}

private struct DateStrip: View {
    @Binding var selection: Date
    var dates: [Date] = DateTools.weekDates()

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 9) {
                ForEach(dates, id: \.self) { date in
                    let active = Calendar.current.isDate(date, inSameDayAs: selection)
                    Button {
                        withAnimation(.spring(response: 0.25, dampingFraction: 0.86)) {
                            selection = date
                        }
                    } label: {
                        VStack(spacing: 5) {
                            Text(DateTools.weekdayLabel(date))
                                .font(.caption2.weight(.bold))
                            Text(DateTools.dayLabel(date))
                                .font(.caption.weight(.black))
                        }
                        .foregroundStyle(active ? Palette.text : Palette.muted)
                        .frame(width: 58, height: 50)
                        .background(active ? Palette.main : Color.white.opacity(0.76), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(active ? Palette.main : Palette.line, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 2)
        }
    }
}

private enum BookingSegment: String, CaseIterable, Identifiable {
    case course
    case coach
    case venue

    var id: String { rawValue }
    var title: String {
        switch self {
        case .course: "团课"
        case .coach: "私教"
        case .venue: "场馆"
        }
    }
}

private enum CourseCategory: String, CaseIterable, Identifiable {
    case all
    case hot
    case fatBurn
    case strength
    case aerobic

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: "全部"
        case .hot: "热门"
        case .fatBurn: "燃脂"
        case .strength: "力量"
        case .aerobic: "有氧"
        }
    }

    var icon: String {
        switch self {
        case .all: "square.grid.2x2"
        case .hot: "flame.fill"
        case .fatBurn: "bolt.heart.fill"
        case .strength: "dumbbell.fill"
        case .aerobic: "figure.run"
        }
    }

    func matches(_ course: CourseSlot) -> Bool {
        switch self {
        case .all:
            return true
        case .hot:
            return course.flashSale == 1 || (course.availableCount ?? 0) <= 6
        case .fatBurn:
            return course.searchText.containsAny(["燃脂", "减脂", "HIIT", "hit", "搏击", "attack"])
        case .strength:
            return course.searchText.containsAny(["力量", "塑形", "训练", "核心", "BODY"])
        case .aerobic:
            return course.searchText.containsAny(["有氧", "舞", "跑", "瑜伽", "普拉提"])
        }
    }
}

private struct CourseBookingList: View {
    @EnvironmentObject private var store: MemberStore
    @State private var category: CourseCategory = .all

    private var sortedCourses: [CourseSlot] {
        store.courses.sorted { ($0.startTime ?? "") < ($1.startTime ?? "") }
    }

    private var featuredCourses: [CourseSlot] {
        Array(sortedCourses.prefix(3))
    }

    private var visibleCourses: [CourseSlot] {
        sortedCourses.filter { category.matches($0) }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 14) {
            CoursePromoCarousel(courses: featuredCourses)

            CourseCategoryStrip(selection: $category)

            HStack(alignment: .firstTextBaseline) {
                Text("可约团课")
                    .font(.title3.weight(.black))
                    .foregroundStyle(Palette.text)
                Text("\(visibleCourses.count) 节")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.muted)
                Spacer()
            }
            .padding(.top, 2)

            if visibleCourses.isEmpty {
                EmptyBlock(text: "暂无符合条件的团课")
            } else {
                ForEach(visibleCourses) { item in
                    CourseBookingCard(course: item)
                }
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct CoursePromoCarousel: View {
    let courses: [CourseSlot]

    private var values: [CourseSlot] {
        courses.isEmpty ? [] : courses
    }

    var body: some View {
        if values.isEmpty {
            CoursePromoPlaceholder()
        } else {
            TabView {
                ForEach(values) { course in
                    CoursePromoCard(course: course)
                        .padding(.horizontal, 1)
                }
            }
            .frame(height: 218)
            .tabViewStyle(.page(indexDisplayMode: .automatic))
        }
    }
}

private struct CoursePromoPlaceholder: View {
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            CourseCover(label: "团课", imageURL: OriginalAssets.courseHero)
            LinearGradient(colors: [.clear, .black.opacity(0.58)], startPoint: .top, endPoint: .bottom)
            VStack(alignment: .leading, spacing: 8) {
                Text("BOOMGYM 团课精选")
                    .font(.title2.weight(.black))
                    .foregroundStyle(.white)
                Text("课程加载后可按类别挑选预约")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.84))
            }
            .padding(16)
        }
        .frame(height: 218)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private struct CoursePromoCard: View {
    let course: CourseSlot

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            CourseCover(
                label: course.flashSale == 1 ? "秒杀" : "推荐",
                imageURL: OriginalAssets.courseHero
            )
            LinearGradient(colors: [.clear, .black.opacity(0.64)], startPoint: .top, endPoint: .bottom)

            VStack(alignment: .leading, spacing: 12) {
                Spacer()
                Text(course.name)
                    .font(.title2.weight(.black))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                HStack(spacing: 10) {
                    Label(DateTools.timeRange(course.startTime, course.endTime), systemImage: "clock")
                    Label(course.coachName ?? "待排教练", systemImage: "person.crop.circle")
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white.opacity(0.86))

                HStack(alignment: .center) {
                    Text(course.venueName ?? "训练区")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.82))
                    Spacer()
                    Text("¥\(course.priceText)")
                        .font(.headline.weight(.black))
                        .foregroundStyle(Palette.orange)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(.white.opacity(0.94), in: Capsule())
                }
            }
            .padding(16)
        }
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private struct CourseCategoryStrip: View {
    @Binding var selection: CourseCategory

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(CourseCategory.allCases) { item in
                    Button {
                        withAnimation(.spring(response: 0.25, dampingFraction: 0.84)) {
                            selection = item
                        }
                    } label: {
                        Label(item.title, systemImage: item.icon)
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(selection == item ? Palette.text : Palette.text.opacity(0.70))
                            .padding(.horizontal, 13)
                            .padding(.vertical, 9)
                            .background(selection == item ? Palette.main.opacity(0.86) : Color.white.opacity(0.78), in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 2)
        }
    }
}

private struct CourseScheduleTopBar: View {
    let weekStart: Date
    let courseCount: Int
    let previousWeek: () -> Void
    let currentWeek: () -> Void
    let nextWeek: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 5) {
                    Text("第 \(DateTools.weekNumber(weekStart)) 周")
                        .font(.system(size: 22, weight: .black, design: .rounded))
                        .foregroundStyle(Palette.orange)
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Palette.text.opacity(0.58))
                }
                Text("\(DateTools.weekRangeTitle(weekStart)) · \(courseCount) 节团课")
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Palette.muted)
            }

            Spacer()

            HStack(spacing: 8) {
                Button(action: previousWeek) {
                    Image(systemName: "chevron.left")
                }
                Button(action: currentWeek) {
                    Text("本周")
                        .font(.caption.weight(.bold))
                }
                Button(action: nextWeek) {
                    Image(systemName: "chevron.right")
                }
            }
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(Palette.text)
        }
        .buttonStyle(.plain)
    }
}

private struct CourseWeekDayHeader: View {
    let weekDates: [Date]

    var body: some View {
        HStack(spacing: 0) {
            Text(DateTools.monthLabel(weekDates.first ?? Date()))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Palette.text.opacity(0.66))
                .frame(width: 52)

            ForEach(weekDates, id: \.self) { date in
                VStack(spacing: 5) {
                    Text(DateTools.weekdaySymbol(date))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Calendar.current.isDateInToday(date) ? Palette.orange : Palette.text)
                    Text(DateTools.dayNumber(date))
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Calendar.current.isDateInToday(date) ? Palette.orange : Palette.muted)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 10)
        .padding(.bottom, 10)
    }
}

private struct CourseWeekGrid: View {
    let weekDates: [Date]
    let courses: [CourseSlot]
    let selectCourse: (CourseSlot) -> Void

    private let startHour = 6
    private let endHour = 22
    private let hourHeight: CGFloat = 74
    private let leftWidth: CGFloat = 52

    private var totalHours: Int {
        endHour - startHour
    }

    private var gridHeight: CGFloat {
        CGFloat(totalHours) * hourHeight
    }

    var body: some View {
        GeometryReader { proxy in
            let columnWidth = max((proxy.size.width - leftWidth) / 7, 34)

            ZStack(alignment: .topLeading) {
                CourseGridLines(
                    totalHours: totalHours,
                    hourHeight: hourHeight,
                    leftWidth: leftWidth,
                    columnWidth: columnWidth
                )

                ForEach(startHour..<endHour, id: \.self) { hour in
                    VStack(alignment: .trailing, spacing: 3) {
                        Text("\(hour)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Palette.text.opacity(0.58))
                        Text(String(format: "%02d:00", hour))
                            .font(.caption2)
                            .foregroundStyle(Palette.muted)
                    }
                    .frame(width: leftWidth - 9, height: hourHeight, alignment: .topTrailing)
                    .offset(x: 0, y: CGFloat(hour - startHour) * hourHeight + 9)
                }

                ForEach(courses) { course in
                    if let frame = eventFrame(for: course, columnWidth: columnWidth) {
                        Button {
                            selectCourse(course)
                        } label: {
                            CourseTimetableEvent(course: course)
                        }
                        .buttonStyle(.plain)
                        .frame(width: max(columnWidth - 8, 28), height: frame.height)
                        .offset(x: frame.x, y: frame.y)
                    }
                }

                if courses.isEmpty {
                    Text("本周暂无团课排期")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Palette.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 42)
                        .offset(x: leftWidth / 2)
                }
            }
            .frame(width: proxy.size.width, height: gridHeight)
        }
        .frame(height: gridHeight)
    }

    private func eventFrame(for course: CourseSlot, columnWidth: CGFloat) -> (x: CGFloat, y: CGFloat, height: CGFloat)? {
        guard let start = DateTools.parse(course.startTime),
              let end = DateTools.parse(course.endTime),
              let dayIndex = weekDates.firstIndex(where: { Calendar.current.isDate(start, inSameDayAs: $0) }) else {
            return nil
        }

        let startMinutes = Calendar.current.component(.hour, from: start) * 60 + Calendar.current.component(.minute, from: start)
        let endMinutes = Calendar.current.component(.hour, from: end) * 60 + Calendar.current.component(.minute, from: end)
        let gridStart = startHour * 60
        let gridEnd = endHour * 60
        let clampedStart = min(max(startMinutes, gridStart), gridEnd)
        let clampedEnd = min(max(endMinutes, gridStart + 20), gridEnd)
        let duration = max(clampedEnd - clampedStart, 35)

        return (
            x: leftWidth + CGFloat(dayIndex) * columnWidth + 4,
            y: CGFloat(clampedStart - gridStart) / 60 * hourHeight + 4,
            height: CGFloat(duration) / 60 * hourHeight - 8
        )
    }
}

private struct CourseGridLines: View {
    let totalHours: Int
    let hourHeight: CGFloat
    let leftWidth: CGFloat
    let columnWidth: CGFloat

    var body: some View {
        ZStack(alignment: .topLeading) {
            ForEach(0...totalHours, id: \.self) { index in
                Rectangle()
                    .fill(Palette.line.opacity(0.62))
                    .frame(height: 1)
                    .offset(x: leftWidth, y: CGFloat(index) * hourHeight)
            }

            ForEach(0...7, id: \.self) { index in
                Rectangle()
                    .fill(Palette.line.opacity(index == 0 ? 0.0 : 0.18))
                    .frame(width: 1)
                    .offset(x: leftWidth + CGFloat(index) * columnWidth, y: 0)
            }
        }
    }
}

private struct CourseTimetableEvent: View {
    let course: CourseSlot

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Capsule()
                .fill(course.accentColor.opacity(0.44))
                .frame(height: 7)
                .padding(.horizontal, -8)
                .padding(.top, -8)

            Text(course.shortScheduleName)
                .font(.caption.weight(.black))
                .foregroundStyle(course.accentColor)
                .lineLimit(4)
                .minimumScaleFactor(0.70)

            Text(course.venueName ?? "训练区")
                .font(.caption2.weight(.medium))
                .foregroundStyle(course.accentColor.opacity(0.76))
                .lineLimit(2)
                .minimumScaleFactor(0.70)
        }
        .padding(8)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(course.tintColor, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct CourseBookingConfirmSheet: View {
    @EnvironmentObject private var store: MemberStore
    @Environment(\.dismiss) private var dismiss
    let course: CourseSlot

    private var conflict: BookingOrder? {
        store.conflictingBooking(startTime: course.startTime, endTime: course.endTime)
    }

    private var seatsAvailable: Bool {
        (course.availableCount ?? 0) > 0
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    CourseCover(
                        label: course.flashSale == 1 ? "秒杀" : "团课",
                        imageURL: OriginalAssets.courseHero
                    )
                    .frame(height: 190)
                    .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))

                    VStack(alignment: .leading, spacing: 10) {
                        HStack(alignment: .firstTextBaseline) {
                            Text(course.name)
                                .font(.title3.weight(.black))
                                .foregroundStyle(Palette.text)
                                .lineLimit(2)
                            Spacer()
                            Text("¥\(course.priceText)")
                                .font(.headline.weight(.black))
                                .foregroundStyle(Palette.orange)
                        }

                        Label(DateTools.range(course.startTime, course.endTime), systemImage: "clock")
                        Label("\(course.venueName ?? "训练区") · 空余 \(course.availableCount ?? 0)/\(course.capacity ?? 0)", systemImage: "mappin.and.ellipse")
                        Label(course.coachName ?? "待排教练", systemImage: "person.crop.circle")
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Palette.muted)

                    if let description = course.description?.nonEmpty {
                        Text(description)
                            .font(.footnote)
                            .foregroundStyle(Palette.muted)
                            .lineSpacing(4)
                    }

                    if let conflict {
                        Label("与 \(conflict.resourceName ?? "已有预约") 冲突", systemImage: "exclamationmark.triangle.fill")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Palette.orange)
                    }

                    Button(seatsAvailable ? "立即预约" : "已满员") {
                        Task {
                            await store.perform {
                                try await store.bookCourse(course)
                            }
                            dismiss()
                        }
                    }
                    .buttonStyle(MainButtonStyle())
                    .disabled(conflict != nil || !seatsAvailable)
                }
                .padding(18)
            }
            .background(Palette.page)
            .navigationTitle("团课预约")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }
}

private struct CourseBookingCard: View {
    @EnvironmentObject private var store: MemberStore
    let course: CourseSlot

    var conflict: BookingOrder? {
        store.conflictingBooking(startTime: course.startTime, endTime: course.endTime)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 13) {
            ZStack(alignment: .bottomLeading) {
                CourseCover(
                    label: course.flashSale == 1 ? "秒杀" : "团课",
                    imageURL: OriginalAssets.courseHero
                )
                .frame(height: 142)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                LinearGradient(colors: [.clear, .black.opacity(0.62)], startPoint: .top, endPoint: .bottom)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(course.name)
                            .font(.title3.weight(.black))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                        Text(DateTools.range(course.startTime, course.endTime))
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Palette.text)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 5)
                            .background(Palette.main, in: Capsule())
                    }
                    Spacer()
                    Text("¥\(course.priceText)")
                        .font(.headline.weight(.black))
                        .foregroundStyle(Palette.orange)
                        .padding(.horizontal, 11)
                        .padding(.vertical, 7)
                        .background(.white.opacity(0.92), in: Capsule())
                }
                .padding(14)
            }

            HStack(spacing: 12) {
                CoachPhoto(name: course.coachName ?? "教练", url: OriginalAssets.courseAvatar(coachName: course.coachName, index: course.id), size: 52)
                VStack(alignment: .leading, spacing: 5) {
                    Text(course.coachName ?? "待排教练")
                        .font(.subheadline.weight(.bold))
                    Label("\(course.venueName ?? "训练区") · 空余 \(course.availableCount ?? 0)/\(course.capacity ?? 0)", systemImage: "figure.run")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                        .lineLimit(1)
                }
                Spacer()
                Button((course.availableCount ?? 0) > 0 ? "立即预约" : "满员") {
                    Task {
                        await store.perform {
                            try await store.bookCourse(course)
                        }
                    }
                }
                .buttonStyle(SmallGreenButtonStyle())
                .disabled(conflict != nil || (course.availableCount ?? 0) <= 0)
            }

            if let conflict {
                Label("与 \(conflict.resourceName ?? "已有预约") 冲突", systemImage: "exclamationmark.triangle.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.orange)
            } else if course.flashSale == 1 {
                Label("秒杀课程，提交后请及时完成支付", systemImage: "bolt.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.orange)
            }
        }
        .padding(14)
        .background(.white.opacity(0.84), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(Palette.main.opacity(0.70), lineWidth: 1))
    }
}

private struct CoachBookingList: View {
    @EnvironmentObject private var store: MemberStore
    let selectedDate: Date

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("私教可约时段")
                        .font(.headline.weight(.black))
                    Text(DateTools.dayTitle(selectedDate))
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
                Spacer()
            }
            .padding(14)
            .appCard(radius: 14)

            if store.coaches.isEmpty {
                EmptyBlock(text: "暂无可约私教")
            } else {
                ForEach(Array(store.coaches.enumerated()), id: \.element.id) { index, coach in
                    CoachBookingCard(coach: coach, index: index, selectedDate: selectedDate)
                }
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct CoachBookingCard: View {
    @EnvironmentObject private var store: MemberStore
    let coach: CoachProfile
    let index: Int
    let selectedDate: Date

    private var daySchedules: [PrivateSchedule] {
        (coach.schedules ?? [])
            .filter { DateTools.isSameDay($0.startTime, as: selectedDate) }
            .sorted { ($0.startTime ?? "") < ($1.startTime ?? "") }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                CoachPhoto(name: coach.name, url: OriginalAssets.coachAvatar(name: coach.name, index: index), size: 64)
                VStack(alignment: .leading, spacing: 5) {
                    Text(coach.name)
                        .font(.headline.weight(.semibold))
                    Text(coach.specialization ?? "私教课程")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                    Text("评分 \(coach.ratingText) · ¥\(money(coach.hourlyPrice))/h")
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
                Spacer()
            }

            if let packages = coach.packages, !packages.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(packages) { item in
                            Text("\(item.name) ¥\(money(item.price))")
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Palette.mainSoft, in: Capsule())
                        }
                    }
                }
            }

            if !daySchedules.isEmpty {
                ForEach(daySchedules) { slot in
                    PrivateSlotRow(slot: slot)
                }
            } else {
                Text("当天暂无开放时段")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
            }
        }
        .padding(14)
        .appCard(radius: 12)
        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.main.opacity(0.75), lineWidth: 1))
    }
}

private struct PrivateSlotRow: View {
    @EnvironmentObject private var store: MemberStore
    let slot: PrivateSchedule

    var conflict: BookingOrder? {
        store.conflictingBooking(startTime: slot.startTime, endTime: slot.endTime)
    }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(DateTools.range(slot.startTime, slot.endTime))
                    .font(.subheadline.weight(.medium))
                Text(conflict == nil ? "可约时段" : "与已有预约冲突")
                    .font(.caption)
                    .foregroundStyle(conflict == nil ? Palette.muted : Palette.orange)
            }
            Spacer()
            Button("预约") {
                Task {
                    await store.perform {
                        try await store.bookPrivate(slot)
                    }
                }
            }
            .buttonStyle(SmallGreenButtonStyle())
            .disabled(conflict != nil || (slot.availableCount ?? 1) <= 0)
        }
        .padding(.vertical, 8)
        .overlay(alignment: .bottom) {
            Divider().overlay(Palette.line)
        }
    }
}

private struct VenueBookingList: View {
    @EnvironmentObject private var store: MemberStore
    let selectedDate: Date

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("场馆场地预约")
                        .font(.headline.weight(.black))
                    Text(DateTools.dayTitle(selectedDate))
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
                Spacer()
            }
            .padding(14)
            .appCard(radius: 14)

            if store.venues.isEmpty {
                EmptyBlock(text: "暂无开放场馆")
            } else {
                ForEach(store.venues) { venue in
                    VenueBookingCard(venue: venue, selectedDate: selectedDate)
                }
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct VenueBookingCard: View {
    @EnvironmentObject private var store: MemberStore
    let venue: Venue
    let selectedDate: Date
    @State private var start: Date
    @State private var hours = 1.0

    init(venue: Venue, selectedDate: Date) {
        self.venue = venue
        self.selectedDate = selectedDate
        _start = State(initialValue: DateTools.nextSlot(on: selectedDate))
    }

    var end: Date { start.addingTimeInterval(hours * 3600) }
    var conflict: BookingOrder? {
        store.conflictingBooking(startTime: DateTools.backendString(start), endTime: DateTools.backendString(end))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ZStack(alignment: .bottomLeading) {
                VenueCover(venue: venue)
                    .frame(height: 138)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                LinearGradient(colors: [.black.opacity(0.02), .black.opacity(0.58)], startPoint: .top, endPoint: .bottom)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(venue.name)
                            .font(.title2.weight(.black))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                        Text("\(venue.location ?? "场馆区域") · 容量 \(venue.capacity ?? 0)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.white.opacity(0.82))
                            .lineLimit(1)
                    }
                    Spacer()
                    Text("¥\(money(venue.pricePerHour))/小时")
                        .font(.headline.weight(.black))
                        .foregroundStyle(Palette.orange)
                        .padding(.horizontal, 11)
                        .padding(.vertical, 7)
                        .background(.white.opacity(0.90), in: Capsule())
                }
                .padding(14)
            }

            Text(venue.description ?? "开放预约")
                .font(.subheadline)
                .foregroundStyle(Palette.muted)
                .lineLimit(2)

            VStack(spacing: 10) {
                HStack(spacing: 8) {
                    Text("开始时间")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Palette.muted)
                    Spacer()
                    StartTimeStepper(start: $start)
                }
                HStack {
                    Text("时长")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Palette.muted)
                    DurationStepper(hours: $hours)
                    Spacer()
                    Button("预约") {
                        Task {
                            await store.perform {
                                try await store.bookVenue(venue, start: start, end: end)
                            }
                        }
                    }
                    .buttonStyle(CompactGreenButtonStyle())
                    .disabled(conflict != nil)
                }
            }
            .padding(.top, 2)

            if let conflict {
                Label("与 \(conflict.resourceName ?? "已有预约") 冲突", systemImage: "exclamationmark.triangle.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.orange)
            } else {
                Label("系统会校验营业时间和已有预约冲突", systemImage: "checkmark.seal.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.muted)
            }
        }
        .padding(14)
        .background(.white.opacity(0.90), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(.white.opacity(0.92), lineWidth: 1))
        .onChange(of: DateTools.startOfDay(selectedDate)) { _, newValue in
            start = DateTools.nextSlot(on: newValue)
        }
    }
}

private struct DurationStepper: View {
    @Binding var hours: Double

    var body: some View {
        HStack(spacing: 8) {
            Button {
                hours = max(0.5, hours - 0.5)
            } label: {
                Image(systemName: "minus")
                    .font(.caption.weight(.bold))
            }
            Text("\(hours, specifier: "%.1f")h")
                .font(.caption.weight(.black))
                .frame(width: 38)
            Button {
                hours = min(4, hours + 0.5)
            } label: {
                Image(systemName: "plus")
                    .font(.caption.weight(.bold))
            }
        }
        .foregroundStyle(Palette.text)
        .padding(.horizontal, 8)
        .frame(height: 30)
        .background(.white.opacity(0.86), in: Capsule())
        .overlay(Capsule().stroke(Palette.line, lineWidth: 1))
        .buttonStyle(.plain)
    }
}

private struct StartTimeStepper: View {
    @Binding var start: Date

    var body: some View {
        HStack(spacing: 8) {
            Button {
                start = max(Date(), start.addingTimeInterval(-1800))
            } label: {
                Image(systemName: "chevron.left")
                    .font(.caption.weight(.bold))
            }
            Text(DateTools.display(start))
                .font(.caption.weight(.black))
                .monospacedDigit()
                .frame(minWidth: 112)
            Button {
                start = start.addingTimeInterval(1800)
            } label: {
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
            }
        }
        .foregroundStyle(Palette.text)
        .buttonStyle(.plain)
    }
}

private struct CompactGreenButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.black))
            .foregroundStyle(Palette.text)
            .padding(.horizontal, 18)
            .frame(height: 36)
            .background(Palette.main.opacity(configuration.isPressed ? 0.72 : 1), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct MineView: View {
    @EnvironmentObject private var store: MemberStore
    @Binding var sheet: MemberSheet?
    let close: () -> Void
    @State private var showMore = false

    private var profileURL: URL? {
        URL(string: store.session?.avatar?.nonEmpty ?? "") ?? OriginalAssets.memberAvatar(index: 0)
    }

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Palette.page.ignoresSafeArea()
            MineHeaderBackdrop()
                .frame(height: 420)
                .ignoresSafeArea(edges: .top)
                .allowsHitTesting(false)

            ScrollView {
                VStack(spacing: 0) {
                    MineImmersiveHeader(
                        name: store.session?.displayName ?? "会员",
                        phone: store.session?.phone ?? "未填写手机号",
                        type: store.home?.activeMembershipName?.nonEmpty ?? "未开通",
                        balance: store.home?.balance ?? store.session?.balance,
                        membership: store.home?.activeMembershipName?.nonEmpty ?? "未开通",
                        privateSessions: store.home?.remainingPrivateSessions ?? 0,
                        bookingCount: store.upcomingBookings.count,
                        avatarURL: profileURL,
                        backAction: close,
                        checkinAction: { sheet = .checkin },
                        walletAction: { sheet = .wallet },
                        moreAction: { showMore = true }
                    )
                    .frame(height: 386)

                    VStack(spacing: 16) {
                        MineActionPanel(
                            walletAction: { sheet = .wallet },
                            checkinAction: { sheet = .checkin },
                            bookingsAction: { sheet = .bookings }
                        )

                        if let booking = store.upcomingBookings.first {
                            MineNextBookingCard(booking: booking) {
                                sheet = .bookings
                            }
                        }

                        MineProfileSummary(
                            username: store.session?.username ?? "-",
                            type: store.home?.activeMembershipName?.nonEmpty ?? "未开通"
                        )
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 108)
                    .background(Palette.page)
                    .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                }
            }

            if showMore {
                Color.black.opacity(0.001)
                    .ignoresSafeArea()
                    .onTapGesture { showMore = false }

                MineMoreMenu(
                    walletAction: {
                        showMore = false
                        sheet = .wallet
                    },
                    checkinAction: {
                        showMore = false
                        sheet = .checkin
                    },
                    bookingsAction: {
                        showMore = false
                        sheet = .bookings
                    },
                    logoutAction: {
                        showMore = false
                        store.logout()
                    }
                )
                .padding(.top, 88)
                .padding(.trailing, 22)
                .transition(.move(edge: .top).combined(with: .opacity))
                .zIndex(2)
            }
        }
        .refreshable { await store.refreshAll() }
        .animation(.spring(response: 0.26, dampingFraction: 0.88), value: showMore)
    }
}

private struct MineMoreMenu: View {
    let walletAction: () -> Void
    let checkinAction: () -> Void
    let bookingsAction: () -> Void
    let logoutAction: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MineMoreRow(icon: "creditcard", title: "钱包资产", action: walletAction)
            Divider().overlay(.white.opacity(0.45))
            MineMoreRow(icon: "qrcode", title: "入场凭证", action: checkinAction)
            Divider().overlay(.white.opacity(0.45))
            MineMoreRow(icon: "calendar", title: "个人日程", action: bookingsAction)
            Divider().overlay(.white.opacity(0.45))
            MineMoreRow(icon: "rectangle.portrait.and.arrow.right", title: "退出登录", role: .destructive, action: logoutAction)
        }
        .frame(width: 176)
        .padding(.vertical, 8)
        .background(Color.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(Palette.line.opacity(0.72), lineWidth: 1))
    }
}

private struct MineMoreRow: View {
    let icon: String
    let title: String
    var role: ButtonRole?
    let action: () -> Void

    var body: some View {
        Button(role: role, action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .frame(width: 20)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Spacer()
            }
            .foregroundStyle(role == .destructive ? Palette.orange : Palette.text)
            .padding(.horizontal, 16)
            .frame(height: 44)
        }
        .buttonStyle(.plain)
    }
}

private struct MineImmersiveHeader: View {
    let name: String
    let phone: String
    let type: String
    let balance: Decimal?
    let membership: String
    let privateSessions: Int
    let bookingCount: Int
    let avatarURL: URL?
    let backAction: () -> Void
    let checkinAction: () -> Void
    let walletAction: () -> Void
    let moreAction: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                HeaderGlassCircleButton(systemImage: "chevron.left", action: backAction)
                Spacer()
                HeaderGlassPill(checkinAction: checkinAction, moreAction: moreAction)
            }

            HStack(spacing: 14) {
                MineAvatar(name: name, url: avatarURL)
                VStack(alignment: .leading, spacing: 8) {
                    Text(name)
                        .font(.system(size: 31, weight: .black, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.74)
                    Text(phone)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Palette.text.opacity(0.64))
                        .lineLimit(1)
                    HStack(spacing: 8) {
                        Text(type)
                            .font(.caption.weight(.black))
                            .padding(.horizontal, 11)
                            .padding(.vertical, 6)
                            .background(Palette.text.opacity(0.88), in: Capsule())
                            .foregroundStyle(.white)
                        Text("Member Pass")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Palette.text.opacity(0.62))
                    }
                }
                Spacer()
            }

            HStack(spacing: 10) {
                MineHeaderMetric(label: "当前会籍", value: membership)
                MineHeaderMetric(label: "私教课时", value: "\(privateSessions)")
                MineHeaderMetric(label: "待参加", value: "\(bookingCount)")
            }

            Button(action: walletAction) {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("钱包余额")
                            .font(.caption)
                            .foregroundStyle(Palette.text.opacity(0.62))
                        Text("¥\(money(balance))")
                            .font(.title2.weight(.black))
                            .foregroundStyle(Palette.text)
                    }
                    Spacer()
                    Text("充值")
                        .font(.subheadline.weight(.black))
                        .foregroundStyle(Palette.text)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .background(Palette.main, in: Capsule())
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(.white.opacity(0.58), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(.white.opacity(0.72), lineWidth: 1))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 22)
        .padding(.top, 0)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

private struct MineHeaderBackdrop: View {
    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .topLeading) {
                LinearGradient(
                    colors: [Palette.main.opacity(0.92), .white, Palette.orange.opacity(0.20)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Capsule()
                    .fill(Color(red: 0.09, green: 0.76, blue: 0.72).opacity(0.38))
                    .frame(width: proxy.size.width * 0.92, height: 54)
                    .rotationEffect(.degrees(-18))
                    .offset(x: -78, y: 18)
                Capsule()
                    .fill(Palette.orange.opacity(0.28))
                    .frame(width: proxy.size.width * 0.78, height: 42)
                    .rotationEffect(.degrees(-12))
                    .offset(x: proxy.size.width * 0.38, y: 66)
                Circle()
                    .fill(.white.opacity(0.42))
                    .frame(width: 170, height: 170)
                    .offset(x: proxy.size.width - 112, y: 116)
            }
        }
    }
}

private struct HeaderGlassCircleButton: View {
    let systemImage: String
    let action: () -> Void

    var body: some View {
        let content = Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 21, weight: .medium))
                .foregroundStyle(Palette.text)
                .frame(width: 42, height: 42)
        }
        .buttonStyle(.plain)
        .contentShape(Circle())

        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.interactive(), in: Circle())
        } else {
            content
                .background(Color.white.opacity(0.58), in: Circle())
                .overlay(Circle().stroke(.white.opacity(0.76), lineWidth: 1))
        }
    }
}

private struct HeaderGlassPill: View {
    let checkinAction: () -> Void
    let moreAction: () -> Void

    var body: some View {
        let content = HStack(spacing: 0) {
            Button(action: checkinAction) {
                Image(systemName: "qrcode.viewfinder")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Palette.text)
                    .frame(width: 42, height: 38)
            }
            .buttonStyle(.plain)

            Divider()
                .frame(height: 18)
                .overlay(.white.opacity(0.60))

            Button(action: moreAction) {
                Image(systemName: "ellipsis")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Palette.text)
                    .frame(width: 40, height: 38)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 4)
        .frame(height: 42)

        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.interactive(), in: Capsule())
        } else {
            content
                .background(Color.white.opacity(0.54), in: Capsule())
                .overlay(Capsule().stroke(.white.opacity(0.82), lineWidth: 1))
        }
    }
}

private struct MineHeaderMetric: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 5) {
            Text(label)
                .font(.caption2.weight(.medium))
                .foregroundStyle(Palette.text.opacity(0.58))
            Text(value)
                .font(.headline.weight(.black))
                .foregroundStyle(Palette.text)
                .lineLimit(1)
                .minimumScaleFactor(0.68)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(.white.opacity(0.46), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(.white.opacity(0.62), lineWidth: 1))
    }
}

private struct MineHeroCard: View {
    let name: String
    let phone: String
    let type: String
    let balance: Decimal?
    let avatarURL: URL?
    let checkinAction: () -> Void
    let walletAction: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .center, spacing: 14) {
                MineAvatar(name: name, url: avatarURL)

                VStack(alignment: .leading, spacing: 9) {
                    Text(name)
                        .font(.system(size: 27, weight: .black, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)

                    HStack(spacing: 8) {
                        Image(systemName: "phone.fill")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Palette.orange)
                        Text(phone)
                            .font(.subheadline)
                            .foregroundStyle(Palette.muted)
                            .lineLimit(1)
                    }
                }

                Spacer()

                Button(action: checkinAction) {
                    Image(systemName: "qrcode")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Palette.text)
                        .frame(width: 42, height: 42)
                        .background(.white.opacity(0.72), in: Circle())
                        .overlay(Circle().stroke(.white.opacity(0.82), lineWidth: 1))
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 10) {
                Text(type)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Palette.text)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(Palette.main, in: Capsule())

                Text("Member Pass")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Palette.muted)

                Spacer()

                Button(action: walletAction) {
                    HStack(spacing: 8) {
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("余额")
                                .font(.caption2)
                                .foregroundStyle(Palette.muted)
                            Text("¥\(money(balance))")
                                .font(.headline.weight(.black))
                                .foregroundStyle(Palette.text)
                        }
                        Text("充值")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Palette.orange)
                    }
                    .padding(.horizontal, 13)
                    .padding(.vertical, 9)
                    .background(.white.opacity(0.70), in: Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(20)
        .background {
            ZStack {
                AsyncImage(url: avatarURL) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill().blur(radius: 18).opacity(0.16)
                    default:
                        Color.clear
                    }
                }
                LinearGradient(
                    colors: [.white.opacity(0.88), Palette.main.opacity(0.30), .white.opacity(0.72)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            }
            .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        }
        .overlay(RoundedRectangle(cornerRadius: 26, style: .continuous).stroke(.white.opacity(0.78), lineWidth: 1))
    }
}

private struct MineAvatar: View {
    let name: String
    let url: URL?

    var body: some View {
        ZStack {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    AvatarView(text: name, index: 0, size: 82)
                }
            }
        }
        .frame(width: 82, height: 82)
        .clipShape(Circle())
        .overlay(Circle().stroke(.white.opacity(0.95), lineWidth: 4))
    }
}

private struct MinePassCard: View {
    let membership: String
    let privateSessions: Int
    let bookingCount: Int

    var body: some View {
        HStack(spacing: 0) {
            MineMetric(label: "当前会籍", value: membership)
            MineMetric(label: "私教课时", value: "\(privateSessions)")
            MineMetric(label: "待参加", value: "\(bookingCount)")
        }
        .padding(.vertical, 16)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(.white.opacity(0.76), lineWidth: 1))
    }
}

private struct MineMetric: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 7) {
            Text(label)
                .font(.caption)
                .foregroundStyle(Palette.muted)
            Text(value)
                .font(.headline.weight(.black))
                .foregroundStyle(Palette.text)
                .lineLimit(1)
                .minimumScaleFactor(0.68)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct MineActionPanel: View {
    let walletAction: () -> Void
    let checkinAction: () -> Void
    let bookingsAction: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SectionTitle("我的权益")

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
                MineActionButton(icon: "creditcard.fill", title: "钱包", action: walletAction)
                MineActionButton(icon: "qrcode", title: "凭证", action: checkinAction)
                MineActionButton(icon: "calendar", title: "日程", action: bookingsAction)
            }
        }
        .padding(16)
        .background(.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(.white.opacity(0.82), lineWidth: 1))
    }
}

private struct SectionTitle: View {
    let title: String
    init(_ title: String) { self.title = title }

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Capsule()
                .fill(Palette.main)
                .frame(width: 30, height: 4)
            Text(title)
                .font(.title3.weight(.black))
        }
    }
}

private struct MineActionButton: View {
    let icon: String
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 21, weight: .semibold))
                    .foregroundStyle(Palette.text)
                    .frame(width: 44, height: 44)
                    .background(Palette.mainSoft, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
                Text(title)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Palette.text)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

private struct MineNextBookingCard: View {
    let booking: BookingOrder
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: "calendar.badge.clock")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(Palette.text)
                    .frame(width: 48, height: 48)
                    .background(Palette.main, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                VStack(alignment: .leading, spacing: 5) {
                    Text(booking.resourceName ?? "下一场预约")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Palette.text)
                        .lineLimit(1)
                    Text(DateTools.range(booking.startTime, booking.endTime))
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                        .lineLimit(1)
                }
                Spacer()
                TagText(text: booking.statusText, color: booking.statusColor)
            }
            .padding(16)
            .background(.white.opacity(0.74), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(.white.opacity(0.82), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

private struct MineProfileSummary: View {
    let username: String
    let type: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.text.rectangle")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Palette.text)
                .frame(width: 44, height: 44)
                .background(Palette.mainSoft, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            VStack(alignment: .leading, spacing: 5) {
                Text(username)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Palette.text)
                Text(type)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Palette.muted)
            }
            Spacer()
            Image(systemName: "ellipsis")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(Palette.muted)
        }
        .padding(16)
        .background(.white.opacity(0.74), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(.white.opacity(0.82), lineWidth: 1))
    }
}

private struct SearchSheet: View {
    @EnvironmentObject private var store: MemberStore
    @Environment(\.dismiss) private var dismiss
    @Binding var sheet: MemberSheet?
    var showsClose = true
    var openBooking: (BookingSegment) -> Void = { _ in }
    @State private var query = ""

    private var keyword: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var courses: [CourseSlot] {
        filter(store.courses) { "\($0.name) \($0.coachName ?? "") \($0.venueName ?? "")" }
    }

    private var coaches: [CoachProfile] {
        filter(store.coaches) { "\($0.name) \($0.specialization ?? "")" }
    }

    private var venues: [Venue] {
        filter(store.venues) { "\($0.name) \($0.location ?? "") \($0.description ?? "")" }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    HStack(spacing: 10) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(Palette.muted)
                        TextField("搜索课程、教练、场馆", text: $query)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 46)
                    .background(.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))

                    SearchSection(title: "课程") {
                        if courses.isEmpty {
                            EmptyText("暂无匹配课程")
                        } else {
                            ForEach(courses.prefix(5)) { course in
                                Button {
                                    route(to: .course)
                                } label: {
                                    SearchRow(title: course.name, subtitle: "\(course.coachName ?? "待排教练") · \(course.venueName ?? "训练区")", icon: "figure.run")
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    SearchSection(title: "教练") {
                        if coaches.isEmpty {
                            EmptyText("暂无匹配教练")
                        } else {
                            ForEach(coaches.prefix(5)) { coach in
                                Button {
                                    route(to: .coach)
                                } label: {
                                    SearchRow(title: coach.name, subtitle: coach.specialization ?? "私教课程", icon: "person.fill")
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    SearchSection(title: "场馆") {
                        if venues.isEmpty {
                            EmptyText("暂无匹配场馆")
                        } else {
                            ForEach(venues.prefix(5)) { venue in
                                Button {
                                    route(to: .venue)
                                } label: {
                                    SearchRow(title: venue.name, subtitle: venue.location ?? "场馆区域", icon: "building.2")
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(Palette.page)
            .navigationTitle("搜索")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }

    private func filter<T>(_ values: [T], text: (T) -> String) -> [T] {
        guard !keyword.isEmpty else { return Array(values.prefix(8)) }
        return values.filter { text($0).localizedCaseInsensitiveContains(keyword) }
    }

    private func route(to segment: BookingSegment) {
        if showsClose {
            dismiss()
            sheet = nil
        }
        openBooking(segment)
    }
}

private struct SearchSection<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(title)
            VStack(spacing: 0) {
                content
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
    }
}

private struct SearchRow: View {
    let title: String
    let subtitle: String
    let icon: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Palette.orange)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Palette.text)
                    .lineLimit(1)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                    .lineLimit(1)
            }
            Spacer()
        }
        .padding(.vertical, 10)
    }
}

private struct WalletSheet: View {
    @EnvironmentObject private var store: MemberStore
    @Environment(\.dismiss) private var dismiss
    var showsClose = true
    @State private var amount = "300"

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    BoxView(title: "钱包资产", more: nil, action: {}) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("余额 ¥\(money(store.home?.balance))")
                                .font(.title2.weight(.semibold))
                            FieldRow(title: "充值金额", text: $amount, icon: "yensign.circle")
                                .keyboardType(.decimalPad)
                            Button("模拟充值") {
                                Task {
                                    await store.perform {
                                        try await store.recharge(amountText: amount)
                                        return "充值成功"
                                    }
                                }
                            }
                            .buttonStyle(MainButtonStyle())
                        }
                    }

                    BoxView(title: "会籍套餐", more: nil, action: {}) {
                        VStack(spacing: 0) {
                            ForEach(Array(store.membershipPackages.enumerated()), id: \.element.id) { index, item in
                                PackageLine(
                                    title: item.name,
                                    subtitle: "\(item.days ?? 0) 天 / 每日 \(item.dailyLimit ?? 0) 次",
                                    price: item.price,
                                    coverURL: OriginalAssets.packageCover(index: index)
                                ) {
                                    Task {
                                        await store.perform {
                                            try await store.purchase(.membership(item))
                                            return "购买成功"
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BoxView(title: "私教课包", more: nil, action: {}) {
                        VStack(spacing: 0) {
                            ForEach(Array(store.privatePackages.enumerated()), id: \.element.id) { index, item in
                                PackageLine(
                                    title: item.name,
                                    subtitle: "\(item.totalSessions ?? 0) 节",
                                    price: item.price,
                                    coverURL: OriginalAssets.packageCover(index: index + 1)
                                ) {
                                    Task {
                                        await store.perform {
                                            try await store.purchase(.privatePackage(item))
                                            return "购买成功"
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BoxView(title: "最近支付单", more: nil, action: {}) {
                        VStack(spacing: 0) {
                            if store.payments.isEmpty {
                                EmptyText("暂无支付单")
                            } else {
                                ForEach(store.payments.prefix(8)) { item in
                                    PaymentLine(payment: item)
                                    if item.id != store.payments.prefix(8).last?.id {
                                        Divider().overlay(Palette.line)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(Palette.page)
            .navigationTitle("钱包资产")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if showsClose {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("关闭") { dismiss() }
                    }
                }
            }
        }
    }
}

private struct BookingsSheet: View {
    @EnvironmentObject private var store: MemberStore
    @Environment(\.dismiss) private var dismiss
    var showsClose = true
    @State private var weekStart = DateTools.startOfWeek(Date())
    @State private var selectedBooking: BookingOrder?

    private var weekDates: [Date] {
        DateTools.weekDates(from: weekStart)
    }

    private var weekBookings: [BookingOrder] {
        store.bookings
            .filter { booking in
                weekDates.contains { DateTools.isSameDay(booking.startTime, as: $0) }
            }
            .sorted { ($0.startTime ?? "") < ($1.startTime ?? "") }
    }

    private var upcomingCount: Int {
        store.bookings.filter { !["CANCELLED", "REFUNDED", "COMPLETED"].contains($0.status ?? "") }.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 12) {
                    MemberScheduleTopBar(
                        weekStart: weekStart,
                        itemCount: weekBookings.count,
                        upcomingCount: upcomingCount,
                        previousWeek: { weekStart = DateTools.addWeeks(-1, to: weekStart) },
                        currentWeek: { weekStart = DateTools.startOfWeek(Date()) },
                        nextWeek: { weekStart = DateTools.addWeeks(1, to: weekStart) }
                    )

                    BookingWeekGrid(
                        weekDates: weekDates,
                        bookings: weekBookings,
                        selectBooking: { selectedBooking = $0 }
                    )

                    if weekBookings.isEmpty {
                        EmptyBlock(text: "本周暂无个人日程")
                    } else {
                        HStack {
                            SectionTitle("本周安排")
                            Spacer()
                        }
                        .padding(.top, 8)

                        ForEach(weekBookings) { booking in
                            BookingRecordCard(booking: booking)
                        }
                    }
                }
                .padding(.horizontal, 10)
                .padding(.top, 8)
                .padding(.bottom, 96)
            }
            .background(Palette.page)
            .toolbar(.hidden, for: .navigationBar)
            .toolbar {
                if showsClose {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("关闭") { dismiss() }
                    }
                }
            }
        }
        .sheet(item: $selectedBooking) { booking in
            BookingRecordCard(booking: booking)
                .padding(16)
                .presentationDetents([.medium])
                .presentationDragIndicator(.visible)
        }
    }
}

private struct MemberScheduleTopBar: View {
    let weekStart: Date
    let itemCount: Int
    let upcomingCount: Int
    let previousWeek: () -> Void
    let currentWeek: () -> Void
    let nextWeek: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "line.3.horizontal")
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(Palette.text.opacity(0.78))
                .frame(width: 30, height: 34)

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 5) {
                    Text("第 \(DateTools.weekNumber(weekStart)) 周")
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .foregroundStyle(Palette.orange)
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Palette.text.opacity(0.58))
                }
                Text(DateTools.weekRangeTitle(weekStart))
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Palette.muted)
            }

            Spacer()

            HStack(spacing: 12) {
                Button(action: previousWeek) {
                    Image(systemName: "chevron.left")
                }
                Button(action: currentWeek) {
                    Text("本周")
                        .font(.caption.weight(.bold))
                        .padding(.horizontal, 9)
                        .padding(.vertical, 6)
                        .background(Palette.main.opacity(0.22), in: Capsule())
                }
                Button(action: nextWeek) {
                    Image(systemName: "chevron.right")
                }
            }
            .font(.system(size: 18, weight: .bold))
            .foregroundStyle(Palette.text)
        }
        .buttonStyle(.plain)
    }
}

private struct ScheduleWeekDayHeader: View {
    let weekDates: [Date]
    var leftWidth: CGFloat = 52
    var dayWidth: CGFloat? = nil

    var body: some View {
        HStack(spacing: 0) {
            Text(DateTools.monthLabel(weekDates.first ?? Date()))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Palette.text.opacity(0.66))
                .frame(width: leftWidth)

            ForEach(weekDates, id: \.self) { date in
                VStack(spacing: 5) {
                    Text(DateTools.weekdaySymbol(date))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Calendar.current.isDateInToday(date) ? Palette.orange : Palette.text)
                    Text(DateTools.dayNumber(date))
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Calendar.current.isDateInToday(date) ? Palette.orange : Palette.muted)
                }
                .frame(width: dayWidth)
            }
        }
        .frame(maxWidth: dayWidth == nil ? .infinity : nil)
        .padding(.vertical, 10)
        .background(.white.opacity(0.82))
    }
}

private struct BookingWeekGrid: View {
    let weekDates: [Date]
    let bookings: [BookingOrder]
    let selectBooking: (BookingOrder) -> Void

    private let startHour = 6
    private let endHour = 22
    private let hourHeight: CGFloat = 78
    private let leftWidth: CGFloat = 52

    private var totalHours: Int {
        endHour - startHour
    }

    private var gridHeight: CGFloat {
        CGFloat(totalHours) * hourHeight
    }

    var body: some View {
        GeometryReader { proxy in
            let columnWidth = max((proxy.size.width - leftWidth) / 7, 42)
            VStack(spacing: 0) {
                ScheduleWeekDayHeader(weekDates: weekDates, leftWidth: leftWidth, dayWidth: columnWidth)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                
                ZStack(alignment: .topLeading) {
                    CourseGridLines(
                        totalHours: totalHours,
                        hourHeight: hourHeight,
                        leftWidth: leftWidth,
                        columnWidth: columnWidth
                    )

                    ForEach(startHour..<endHour, id: \.self) { hour in
                        VStack(alignment: .trailing, spacing: 3) {
                            Text("\(hour)")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Palette.text.opacity(0.58))
                            Text(String(format: "%02d:00", hour))
                                .font(.caption2)
                                .foregroundStyle(Palette.muted)
                        }
                        .frame(width: leftWidth - 9, height: hourHeight, alignment: .topTrailing)
                        .offset(x: 0, y: CGFloat(hour - startHour) * hourHeight + 9)
                    }

                    ForEach(bookings) { booking in
                        if let frame = eventFrame(for: booking, columnWidth: columnWidth) {
                            Button {
                                selectBooking(booking)
                            } label: {
                                BookingTimetableEvent(booking: booking)
                            }
                            .buttonStyle(.plain)
                            .frame(width: max(columnWidth - 10, 32), height: frame.height)
                            .offset(x: frame.x, y: frame.y)
                        }
                    }

                    if bookings.isEmpty {
                        Text("本周暂无日程")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Palette.muted)
                            .frame(width: columnWidth * 7, alignment: .center)
                            .padding(.top, 42)
                            .offset(x: leftWidth)
                    }
                }
                .frame(width: proxy.size.width, height: gridHeight)
            }
            .frame(width: proxy.size.width, height: gridHeight + 62)
        }
        .frame(height: gridHeight + 62)
        .background(Color.white.opacity(0.72), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func eventFrame(for booking: BookingOrder, columnWidth: CGFloat) -> (x: CGFloat, y: CGFloat, height: CGFloat)? {
        guard let start = DateTools.parse(booking.startTime),
              let end = DateTools.parse(booking.endTime),
              let dayIndex = weekDates.firstIndex(where: { Calendar.current.isDate(start, inSameDayAs: $0) }) else {
            return nil
        }

        let startMinutes = Calendar.current.component(.hour, from: start) * 60 + Calendar.current.component(.minute, from: start)
        let endMinutes = Calendar.current.component(.hour, from: end) * 60 + Calendar.current.component(.minute, from: end)
        let gridStart = startHour * 60
        let gridEnd = endHour * 60
        let clampedStart = min(max(startMinutes, gridStart), gridEnd)
        let clampedEnd = min(max(endMinutes, gridStart + 20), gridEnd)
        let nextConflictingStart = nextConflictingStartMinute(after: start, before: end, dayIndex: dayIndex, currentId: booking.id)
        let visualEnd = nextConflictingStart.map { min(clampedEnd, max(clampedStart + 25, $0 - 5)) } ?? clampedEnd
        let duration = max(visualEnd - clampedStart, 25)
        let adjacentOffset = adjacentEarlierEventCount(before: start, dayIndex: dayIndex) * 10
        let minimumHeight: CGFloat = nextConflictingStart == nil ? 50 : 36

        return (
            x: leftWidth + CGFloat(dayIndex) * columnWidth + 5,
            y: CGFloat(clampedStart - gridStart) / 60 * hourHeight + 9 + CGFloat(adjacentOffset),
            height: max(CGFloat(duration) / 60 * hourHeight - 18, minimumHeight)
        )
    }

    private func nextConflictingStartMinute(after start: Date, before end: Date, dayIndex: Int, currentId: Int) -> Int? {
        bookings.compactMap { other -> Int? in
            guard other.id != currentId,
                  let otherStart = DateTools.parse(other.startTime),
                  let otherDayIndex = weekDates.firstIndex(where: { Calendar.current.isDate(otherStart, inSameDayAs: $0) }),
                  otherDayIndex == dayIndex,
                  otherStart > start,
                  otherStart < end else {
                return nil
            }
            return Calendar.current.component(.hour, from: otherStart) * 60 + Calendar.current.component(.minute, from: otherStart)
        }
        .min()
    }

    private func adjacentEarlierEventCount(before start: Date, dayIndex: Int) -> Int {
        bookings.filter { other in
            guard let otherStart = DateTools.parse(other.startTime),
                  let otherEnd = DateTools.parse(other.endTime),
                  let otherDayIndex = weekDates.firstIndex(where: { Calendar.current.isDate(otherStart, inSameDayAs: $0) }) else {
                return false
            }
            return otherDayIndex == dayIndex
                && otherEnd <= start
                && start.timeIntervalSince(otherEnd) < 90
        }.count
    }
}

private struct BookingTimetableEvent: View {
    let booking: BookingOrder

    private var accent: Color {
        switch booking.resourceType {
        case "PRIVATE_COACH": Palette.orange
        case "VENUE": Color(red: 0.16, green: 0.54, blue: 0.96)
        case "GROUP_COURSE": Color(red: 0.42, green: 0.40, blue: 0.92)
        default: Palette.text
        }
    }

    private var tint: Color {
        switch booking.resourceType {
        case "PRIVATE_COACH": Color(red: 1.0, green: 0.93, blue: 0.86)
        case "VENUE": Color(red: 0.90, green: 0.95, blue: 1.0)
        case "GROUP_COURSE": Color(red: 0.94, green: 0.91, blue: 1.0)
        default: Palette.mainSoft
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Capsule()
                .fill(accent.opacity(0.44))
                .frame(height: 4)
                .padding(.horizontal, 2)

            Text((booking.resourceName ?? "预约").scheduleBlockText())
                .font(.system(size: 11, weight: .black))
                .foregroundStyle(accent)
                .lineLimit(7)
                .minimumScaleFactor(0.72)
                .fixedSize(horizontal: false, vertical: true)

            Text(DateTools.timeRange(booking.startTime, booking.endTime))
                .font(.system(size: 8, weight: .semibold))
                .foregroundStyle(accent.opacity(0.72))
                .lineLimit(2)
                .minimumScaleFactor(0.55)
        }
        .padding(6)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(tint.opacity(0.92), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(accent.opacity(0.10), lineWidth: 1))
    }
}

private struct BookingRecordCard: View {
    @EnvironmentObject private var store: MemberStore
    let booking: BookingOrder
    @State private var showToken = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text(booking.resourceName ?? "预约")
                        .font(.headline.weight(.semibold))
                    Text(DateTools.range(booking.startTime, booking.endTime))
                        .font(.caption)
                        .foregroundStyle(Palette.muted)
                }
                Spacer()
                TagText(text: booking.statusText, color: booking.statusColor)
            }
            Text("金额 ¥\(money(booking.amount))，支付 \(booking.paymentStatusText)")
                .font(.caption)
                .foregroundStyle(Palette.muted)
            HStack {
                if booking.canPay {
                    Button("支付") {
                        Task {
                            await store.perform {
                                try await store.payBooking(booking)
                                return "支付成功"
                            }
                        }
                    }
                    .buttonStyle(SmallGreenButtonStyle())
                }
                if booking.canCheckin {
                    Button("签到码") {
                        showToken = true
                    }
                    .buttonStyle(SmallGreenButtonStyle())
                }
                if booking.canCancel {
                    Button("取消预约") {
                        Task {
                            await store.perform {
                                try await store.cancelBooking(booking)
                                return "预约已取消"
                            }
                        }
                    }
                    .buttonStyle(SmallOrangeButtonStyle())
                }
            }
        }
        .padding(14)
        .appCard(radius: 12)
        .sheet(isPresented: $showToken) {
            CheckinTokenView(booking: booking)
        }
    }
}

private enum CheckinMode {
    case code
    case scan
}

private struct CheckinSheet: View {
    @EnvironmentObject private var store: MemberStore
    @Environment(\.dismiss) private var dismiss
    @State var mode: CheckinMode
    var showsClose = true
    @State private var manualToken = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    if mode == .code {
                        let checkable = store.bookings.filter(\.canCheckin)
                        if checkable.isEmpty {
                            EmptyBlock(text: "暂无可签到预约")
                        } else {
                            ForEach(checkable) { booking in
                                BookingRecordCard(booking: booking)
                            }
                        }
                    } else {
                        QRScannerView { token in
                            manualToken = token
                        }
                        .frame(height: 260)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                        FieldRow(title: "签到 token", text: $manualToken, icon: "qrcode")
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()

                        Button("核销签到") {
                            Task {
                                await store.perform {
                                    try await store.consumeCheckin(token: manualToken)
                                    manualToken = ""
                                    return "核销成功"
                                }
                            }
                        }
                        .buttonStyle(MainButtonStyle())

                        Text("会员端只能核销自己的签到码；管理员核销仍使用后台入口。")
                            .font(.footnote)
                            .foregroundStyle(Palette.muted)
                    }
                }
                .padding(16)
            }
            .background(Palette.page)
            .navigationTitle("签到核销")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if showsClose {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("关闭") { dismiss() }
                    }
                }
            }
        }
    }
}

private struct CheckinTokenView: View {
    @EnvironmentObject private var store: MemberStore
    let booking: BookingOrder
    @State private var token: CheckinToken?

    var body: some View {
        VStack(spacing: 18) {
            Text(booking.resourceName ?? "预约签到")
                .font(.title3.weight(.semibold))
            Text(DateTools.range(booking.startTime, booking.endTime))
                .font(.caption)
                .foregroundStyle(Palette.muted)

            if let token {
                QRCodeImage(text: token.token)
                    .frame(width: 220, height: 220)
                    .padding(16)
                    .background(.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                Text("有效期至 \(DateTools.display(token.expireTime))")
                    .font(.footnote)
                    .foregroundStyle(Palette.muted)
                Text(token.token)
                    .font(.caption2.monospaced())
                    .foregroundStyle(Palette.muted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            } else {
                ProgressView().tint(Palette.main)
            }
        }
        .padding(24)
        .background(Palette.page)
        .task {
            await store.perform(showBusy: false) {
                token = try await store.createCheckinToken(booking)
                return nil
            }
        }
    }
}

private struct PackageLine: View {
    let title: String
    let subtitle: String
    let price: Decimal?
    let coverURL: URL?
    let action: () -> Void

    init(title: String, subtitle: String, price: Decimal?, coverURL: URL? = nil, action: @escaping () -> Void) {
        self.title = title
        self.subtitle = subtitle
        self.price = price
        self.coverURL = coverURL
        self.action = action
    }

    var body: some View {
        HStack(spacing: 12) {
            PackageThumb(url: coverURL)
                .frame(width: 76, height: 48)
            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
                    .lineLimit(1)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 6) {
                Text("¥\(money(price))")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Palette.orange)
                Button("购买", action: action)
                    .buttonStyle(SmallGreenButtonStyle())
            }
        }
        .padding(.vertical, 10)
    }
}

private struct PaymentLine: View {
    @EnvironmentObject private var store: MemberStore
    let payment: PaymentOrder

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 5) {
                Text(payment.paymentTypeText)
                    .font(.subheadline.weight(.semibold))
                Text("¥\(money(payment.amount)) · \(payment.statusText)")
                    .font(.caption)
                    .foregroundStyle(Palette.muted)
            }
            Spacer()
            if payment.status == "UNPAID" {
                Button("支付") {
                    Task {
                        await store.perform {
                            try await store.payPayment(payment)
                            return "支付成功"
                        }
                    }
                }
                .buttonStyle(SmallGreenButtonStyle())
            }
        }
        .padding(.vertical, 10)
    }
}

private struct MineMenuItem: View {
    let icon: String
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(Palette.text)
                Text(title)
                    .font(.caption)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

private struct AccountRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundStyle(Palette.muted)
            Spacer()
            Text(value)
        }
        .font(.subheadline)
    }
}

private struct FieldRow: View {
    let title: String
    @Binding var text: String
    let icon: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .foregroundStyle(Palette.orange)
                .frame(width: 22)
            TextField(title, text: $text)
                .foregroundStyle(Palette.text)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Palette.line, lineWidth: 1))
    }
}

private struct SecureFieldRow: View {
    let title: String
    @Binding var text: String
    let icon: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .foregroundStyle(Palette.orange)
                .frame(width: 22)
            SecureField(title, text: $text)
                .foregroundStyle(Palette.text)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.white, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Palette.line, lineWidth: 1))
    }
}

private struct EmptyText: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(Palette.muted)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
    }
}

private struct EmptyBlock: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(Palette.muted)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 40)
            .appCard(radius: 12)
    }
}

private struct TagText: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundStyle(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color, in: Capsule())
    }
}

private struct CachedRemoteImage<Placeholder: View>: View {
    let url: URL?
    let contentMode: ContentMode
    @ViewBuilder let placeholder: Placeholder
    @State private var image: UIImage?
    @State private var loadedURL: URL?

    init(url: URL?, contentMode: ContentMode = .fill, @ViewBuilder placeholder: () -> Placeholder) {
        self.url = url
        self.contentMode = contentMode
        self.placeholder = placeholder()
    }

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: contentMode)
            } else {
                placeholder
            }
        }
        .task(id: url) { await load() }
    }

    private func load() async {
        guard let url else {
            image = nil
            loadedURL = nil
            return
        }
        guard loadedURL != url || image == nil else { return }

        let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad, timeoutInterval: 12)
        if let cached = imageCache.cachedResponse(for: request),
           let uiImage = await decodedDisplayImage(from: cached.data) {
            image = uiImage
            loadedURL = url
            return
        }
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            imageCache.storeCachedResponse(CachedURLResponse(response: response, data: data), for: request)
            image = await decodedDisplayImage(from: data)
            loadedURL = url
        } catch {
            image = nil
            loadedURL = nil
        }
    }
}

private struct CourseCover: View {
    let label: String
    let imageURL: URL?

    init(label: String, imageURL: URL? = nil) {
        self.label = label
        self.imageURL = imageURL
    }

    var body: some View {
        ZStack {
            CachedRemoteImage(url: imageURL) {
                LinearGradient(colors: [Palette.main.opacity(0.85), Palette.orange.opacity(0.58)], startPoint: .topLeading, endPoint: .bottomTrailing)
            }
            LinearGradient(colors: [.black.opacity(0.02), .black.opacity(0.32)], startPoint: .top, endPoint: .bottom)
            Text(label)
                .font(.caption.weight(.black))
                .foregroundStyle(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Palette.orange.opacity(0.88), in: Capsule())
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                .padding(7)
        }
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

private struct VenueCover: View {
    let venue: Venue?

    var body: some View {
        if let url = venue?.coverImageURL {
            CachedRemoteImage(url: url) {
                placeholder
            }
        } else {
            placeholder
        }
    }

    private var placeholder: some View {
        ZStack {
            LinearGradient(colors: [Palette.main.opacity(0.85), .white, Palette.orange.opacity(0.35)], startPoint: .topLeading, endPoint: .bottomTrailing)
            CachedRemoteImage(url: OriginalAssets.venueCover(index: venue?.id ?? 0)) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.largeTitle)
                    .foregroundStyle(Palette.text.opacity(0.28))
            }
            LinearGradient(colors: [.clear, .black.opacity(0.28)], startPoint: .top, endPoint: .bottom)
        }
    }
}

private struct CoachPhoto: View {
    let name: String
    let url: URL?
    let size: CGFloat

    var body: some View {
        ZStack {
            CachedRemoteImage(url: url) {
                AvatarView(text: name, index: name.count, size: size)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(.white, lineWidth: 3))
    }
}

private struct PackageThumb: View {
    let url: URL?

    var body: some View {
        ZStack {
            CachedRemoteImage(url: url) {
                LinearGradient(colors: [Palette.text.opacity(0.88), Palette.orange.opacity(0.68)], startPoint: .topLeading, endPoint: .bottomTrailing)
                    .overlay(alignment: .bottomLeading) {
                        Image(systemName: "creditcard.fill")
                            .foregroundStyle(.white.opacity(0.9))
                            .padding(8)
                    }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct AvatarView: View {
    let text: String
    let index: Int
    let size: CGFloat

    var body: some View {
        ZStack {
            Circle()
                .fill(index.isMultiple(of: 2) ? Palette.mainSoft : Palette.orange.opacity(0.18))
            Text(String(text.prefix(1)))
                .font(.system(size: size * 0.38, weight: .bold))
                .foregroundStyle(Palette.text)
        }
        .frame(width: size, height: size)
        .overlay(Circle().stroke(.white, lineWidth: 3))
    }
}

private struct MainButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline.weight(.semibold))
            .foregroundStyle(Palette.text)
            .padding(.vertical, 13)
            .padding(.horizontal, 16)
            .background(Palette.main.opacity(configuration.isPressed ? 0.72 : 1), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

private struct OutlineButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.medium))
            .foregroundStyle(Palette.text)
            .padding(.vertical, 11)
            .padding(.horizontal, 16)
            .background(Color.white.opacity(configuration.isPressed ? 0.72 : 1), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Palette.line, lineWidth: 1))
    }
}

private struct SmallGreenButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.caption.weight(.semibold))
            .foregroundStyle(Palette.text)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(Palette.main.opacity(configuration.isPressed ? 0.72 : 1), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct SmallOrangeButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.caption.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(Palette.orange.opacity(configuration.isPressed ? 0.72 : 1), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct RoundBookButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.caption.weight(.semibold))
            .foregroundStyle(Palette.text)
            .frame(width: 54, height: 54)
            .background(Palette.main.opacity(configuration.isPressed ? 0.72 : 1), in: Circle())
    }
}

private extension View {
    func appCard(radius: CGFloat) -> some View {
        background(Palette.card, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
    }

    func homeGlassCapsule(fill: Color = .white.opacity(0.72), stroke: Color = .white.opacity(0.82)) -> some View {
        modifier(HomeGlassCapsuleModifier(fill: fill, stroke: stroke))
    }
}

private struct HomeGlassCapsuleModifier: ViewModifier {
    let fill: Color
    let stroke: Color

    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.tint(.white.opacity(0.16)).interactive(), in: Capsule())
                .overlay {
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [.white.opacity(0.64), .white.opacity(0.06), .clear],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .allowsHitTesting(false)
                }
                .overlay(Capsule().stroke(stroke, lineWidth: 1.25))
                .overlay(Capsule().stroke(.white.opacity(0.42), lineWidth: 3).blur(radius: 1.2).padding(1.5))
                .overlay(Capsule().stroke(.black.opacity(0.08), lineWidth: 0.5).padding(1))
                .shadow(color: .black.opacity(0.20), radius: 14, x: 0, y: 7)
        } else {
            content
                .background(.ultraThinMaterial, in: Capsule())
                .background(fill, in: Capsule())
                .overlay {
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [.white.opacity(0.62), .clear, .black.opacity(0.08)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .allowsHitTesting(false)
                }
                .overlay(Capsule().stroke(stroke, lineWidth: 1.25))
                .overlay(Capsule().stroke(.white.opacity(0.36), lineWidth: 2.5).blur(radius: 1.0).padding(1.5))
                .overlay(Capsule().stroke(.black.opacity(0.08), lineWidth: 0.5).padding(1))
                .shadow(color: .black.opacity(0.14), radius: 10, x: 0, y: 5)
        }
    }
}

@MainActor
private final class MemberStore: ObservableObject {
    @Published var session: UserSession?
    @Published var home: MemberHome?
    @Published var venues: [Venue] = []
    @Published var courses: [CourseSlot] = []
    @Published var coaches: [CoachProfile] = []
    @Published var bookings: [BookingOrder] = []
    @Published var payments: [PaymentOrder] = []
    @Published var membershipPackages: [MembershipPackage] = []
    @Published var privatePackages: [PrivatePackage] = []
    @Published var coachDashboard: CoachDashboard?
    @Published var coachSchedules: [CoachScheduleItem] = []
    @Published var coachBookings: [BookingOrder] = []
    @Published var trainingLogs: [TrainingLog] = []
    @Published var isWorking = false
    @Published var alert: String?
    @Published var toast: String?

    private let api = APIClient()
    private let defaults = UserDefaults.standard
    private let sessionKey = "boomgym.member.session"
    private let baseURLKey = "boomgym.member.baseURL"

    var baseURLString: String {
        defaults.string(forKey: baseURLKey) ?? "http://127.0.0.1:9090"
    }

    var upcomingBookings: [BookingOrder] {
        bookings
            .filter { ["CREATED", "PENDING_PAY", "CONFIRMED"].contains($0.status ?? "") }
            .sorted { ($0.startTime ?? "") < ($1.startTime ?? "") }
    }

    var pendingCoachBookings: [BookingOrder] {
        coachBookings
            .filter { $0.status == "CREATED" && $0.resourceType == "PRIVATE_COACH" }
            .sorted { ($0.startTime ?? "") < ($1.startTime ?? "") }
    }

    func bootstrap() async {
        api.baseURL = baseURLString
        if let data = defaults.data(forKey: sessionKey),
           let cached = try? JSONDecoder().decode(UserSession.self, from: data) {
            session = cached
            api.token = cached.token
            await refreshAll(showErrors: false)
        }
    }

    func login(username: String, password: String, baseURL: String) async throws {
        let normalized = normalizedBaseURL(baseURL)
        api.baseURL = normalized
        let user: UserSession = try await api.post("/user/login", body: LoginRequest(username: username, password: password))
        session = user
        api.token = user.token
        defaults.set(normalized, forKey: baseURLKey)
        if let encoded = try? JSONEncoder().encode(user) {
            defaults.set(encoded, forKey: sessionKey)
        }
        try await refreshAllThrowing()
    }

    func logout() {
        session = nil
        home = nil
        venues = []
        courses = []
        coaches = []
        bookings = []
        payments = []
        membershipPackages = []
        privatePackages = []
        coachDashboard = nil
        coachSchedules = []
        coachBookings = []
        trainingLogs = []
        api.token = nil
        defaults.removeObject(forKey: sessionKey)
    }

    func refreshAll(showErrors: Bool = true) async {
        do {
            try await refreshAllThrowing()
        } catch {
            if showErrors {
                alert = message(for: error)
            }
        }
    }

    func refreshAllThrowing() async throws {
        guard let session else { return }
        if session.isCoach {
            try await refreshCoachThrowing()
            return
        }
        try await refreshMemberThrowing()
    }

    private func refreshMemberThrowing() async throws {
        async let home: MemberHome = api.get("/member/home")
        async let venues: [Venue] = api.get("/member/venues")
        async let courses: [CourseSlot] = api.get("/member/group-courses")
        async let coaches: [CoachProfile] = api.get("/member/coaches")
        async let bookings: [BookingOrder] = api.get("/member/bookings")
        async let payments: [PaymentOrder] = api.get("/member/payments")
        async let memberships: [MembershipPackage] = api.get("/member/membership-packages")
        async let privates: [PrivatePackage] = api.get("/member/private-packages")

        self.home = try await home
        self.venues = try await venues
        self.courses = try await courses
        self.coaches = try await coaches
        self.bookings = try await bookings
        self.payments = try await payments
        self.membershipPackages = try await memberships
        self.privatePackages = try await privates
    }

    private func refreshCoachThrowing() async throws {
        async let dashboard: CoachDashboard = api.get("/coach/home")
        async let schedules: [CoachScheduleItem] = api.get("/coach/schedules")
        async let bookings: [BookingOrder] = api.get("/coach/bookings")
        async let logs: [TrainingLog] = api.get("/coach/training-logs")

        self.coachDashboard = try await dashboard
        self.coachSchedules = try await schedules
        self.coachBookings = try await bookings
        self.trainingLogs = try await logs
    }

    func perform(showBusy: Bool = true, _ operation: () async throws -> String?) async {
        if showBusy { isWorking = true }
        defer { if showBusy { isWorking = false } }
        do {
            if let result = try await operation() {
                showToast(result)
            }
        } catch {
            alert = message(for: error)
        }
    }

    func bookCourse(_ course: CourseSlot) async throws -> String {
        try validateAvailable(course.availableCount, "团课名额不足")
        try validateNoConflict(startTime: course.startTime, endTime: course.endTime)
        let response: BookingCreateResponse = try await api.post("/member/bookings", body: BookingCreateRequest(
            resourceType: "GROUP_COURSE",
            resourceId: course.id,
            source: "IOS",
            idempotentKey: "ios-course-\(course.id)-\(Int(Date().timeIntervalSince1970))",
            startTime: nil,
            endTime: nil
        ))
        try await refreshAllThrowing()
        return bookingResultMessage(response, confirmedText: "团课预约已确认")
    }

    func bookPrivate(_ slot: PrivateSchedule) async throws -> String {
        try validateAvailable(slot.availableCount, "私教时段已满")
        try validateNoConflict(startTime: slot.startTime, endTime: slot.endTime)
        let response: BookingCreateResponse = try await api.post("/member/bookings", body: BookingCreateRequest(
            resourceType: "PRIVATE_COACH",
            resourceId: slot.id,
            source: "IOS",
            idempotentKey: "ios-private-\(slot.id)-\(Int(Date().timeIntervalSince1970))",
            startTime: nil,
            endTime: nil
        ))
        try await refreshAllThrowing()
        return bookingResultMessage(response, confirmedText: "私教预约已提交")
    }

    func bookVenue(_ venue: Venue, start: Date, end: Date) async throws -> String {
        guard end > start else { throw AppError.message("结束时间必须晚于开始时间") }
        try validateVenueWindow(venue, start: start, end: end)
        let startText = DateTools.backendString(start)
        let endText = DateTools.backendString(end)
        try validateNoConflict(startTime: startText, endTime: endText)
        let response: BookingCreateResponse = try await api.post("/member/bookings", body: BookingCreateRequest(
            resourceType: "VENUE",
            resourceId: venue.id,
            source: "IOS",
            idempotentKey: "ios-venue-\(venue.id)-\(Int(Date().timeIntervalSince1970))",
            startTime: startText,
            endTime: endText
        ))
        try await refreshAllThrowing()
        return bookingResultMessage(response, confirmedText: "场馆预约已确认")
    }

    func cancelBooking(_ booking: BookingOrder) async throws {
        let _: Bool = try await api.postEmpty("/member/bookings/\(booking.id)/cancel")
        try await refreshAllThrowing()
    }

    func payBooking(_ booking: BookingOrder) async throws {
        let payment: PaymentOrder = try await api.post("/member/payments", body: PaymentCreateRequest(paymentType: "BOOKING", targetId: booking.id, amount: nil))
        let _: PaymentOrder = try await api.postEmpty("/member/payments/\(payment.paymentNo)/pay")
        try await refreshAllThrowing()
    }

    func recharge(amountText: String) async throws {
        guard let amount = Decimal(string: amountText.trimmingCharacters(in: .whitespacesAndNewlines)), amount > 0 else {
            throw AppError.message("充值金额必须大于 0")
        }
        let payment: PaymentOrder = try await api.post("/member/payments", body: PaymentCreateRequest(paymentType: "RECHARGE", targetId: nil, amount: amount))
        let _: PaymentOrder = try await api.postEmpty("/member/payments/\(payment.paymentNo)/pay")
        try await refreshAllThrowing()
    }

    func purchase(_ item: Purchasable) async throws {
        let request: PaymentCreateRequest
        switch item {
        case .membership(let membership):
            request = PaymentCreateRequest(paymentType: "MEMBERSHIP", targetId: membership.id, amount: nil)
        case .privatePackage(let package):
            request = PaymentCreateRequest(paymentType: "PRIVATE_PACKAGE", targetId: package.id, amount: nil)
        }
        let payment: PaymentOrder = try await api.post("/member/payments", body: request)
        let _: PaymentOrder = try await api.postEmpty("/member/payments/\(payment.paymentNo)/pay")
        try await refreshAllThrowing()
    }

    func payPayment(_ payment: PaymentOrder) async throws {
        let _: PaymentOrder = try await api.postEmpty("/member/payments/\(payment.paymentNo)/pay")
        try await refreshAllThrowing()
    }

    func createCheckinToken(_ booking: BookingOrder) async throws -> CheckinToken {
        try await api.get("/member/checkin/\(booking.id)")
    }

    func consumeCheckin(token: String) async throws {
        let text = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { throw AppError.message("签到 token 不能为空") }
        let _: Bool = try await api.post("/member/checkin/consume", body: CheckinConsumeRequest(token: text))
        try await refreshAllThrowing()
    }

    func reviewCoachBooking(_ booking: BookingOrder, approved: Bool) async throws {
        guard booking.status == "CREATED" else {
            throw AppError.message("该预约已处理")
        }
        let action = approved ? "approve" : "reject"
        let _: Bool = try await api.postEmpty("/coach/bookings/\(booking.id)/\(action)")
        try await refreshAllThrowing()
    }

    func saveTrainingLog(
        userIdText: String,
        bookingIdText: String,
        trainDate: Date,
        focusArea: String,
        content: String,
        remark: String
    ) async throws {
        let userId = Int(userIdText.trimmingCharacters(in: .whitespacesAndNewlines))
        guard let userId, userId > 0 else {
            throw AppError.message("会员ID必须为有效数字")
        }
        let bookingId = Int(bookingIdText.trimmingCharacters(in: .whitespacesAndNewlines))
        let cleanFocus = focusArea.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanFocus.isEmpty else {
            throw AppError.message("训练重点不能为空")
        }
        guard !cleanContent.isEmpty else {
            throw AppError.message("训练内容不能为空")
        }
        let request = TrainingLogCreateRequest(
            userId: userId,
            bookingOrderId: bookingId,
            trainDate: DateTools.backendDateString(trainDate),
            focusArea: cleanFocus,
            content: cleanContent,
            remark: remark.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty
        )
        let _: Bool = try await api.post("/coach/training-logs", body: request)
        try await refreshAllThrowing()
    }

    func conflictingBooking(startTime: String?, endTime: String?) -> BookingOrder? {
        guard let start = DateTools.parse(startTime), let end = DateTools.parse(endTime) else { return nil }
        return bookings.first { booking in
            guard ["CREATED", "PENDING_PAY", "CONFIRMED", "CHECKED_IN"].contains(booking.status ?? ""),
                  let bookingStart = DateTools.parse(booking.startTime),
                  let bookingEnd = DateTools.parse(booking.endTime) else {
                return false
            }
            return bookingStart < end && bookingEnd > start
        }
    }

    private func validateAvailable(_ count: Int?, _ text: String) throws {
        if let count, count <= 0 {
            throw AppError.message(text)
        }
    }

    private func validateNoConflict(startTime: String?, endTime: String?) throws {
        if let conflict = conflictingBooking(startTime: startTime, endTime: endTime) {
            throw AppError.message("个人日程冲突：已预约 \(conflict.resourceName ?? "其他项目")")
        }
    }

    private func validateVenueWindow(_ venue: Venue, start: Date, end: Date) throws {
        guard Calendar.current.isDate(start, inSameDayAs: end) else {
            throw AppError.message("场馆预约不能跨天")
        }
        if let open = venue.openTime, DateTools.clock(start) < open {
            throw AppError.message("预约开始时间早于场馆开放时间")
        }
        if let close = venue.closeTime, DateTools.clock(end) > close {
            throw AppError.message("预约结束时间晚于场馆关闭时间")
        }
    }

    private func bookingResultMessage(_ response: BookingCreateResponse, confirmedText: String) -> String {
        if response.paymentOrder != nil || response.booking.canPay {
            return "已锁定名额，请在预约记录完成支付"
        }
        if response.booking.status == "CREATED" {
            return response.message?.nonEmpty ?? "已提交，等待教练确认"
        }
        return response.message?.nonEmpty ?? confirmedText
    }

    private func normalizedBaseURL(_ value: String) -> String {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "http://127.0.0.1:9090" }
        return trimmed.replacingOccurrences(of: "/+$", with: "", options: .regularExpression)
    }

    private func showToast(_ text: String) {
        toast = text
        Task {
            try? await Task.sleep(nanoseconds: 1_800_000_000)
            if toast == text {
                toast = nil
            }
        }
    }

    private func message(for error: Error) -> String {
        if let error = error as? APIError { return error.localizedDescription }
        if let error = error as? AppError { return error.localizedDescription }
        return error.localizedDescription
    }
}

private final class APIClient {
    var baseURL = "http://127.0.0.1:9090"
    var token: String?

    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    func get<T: Decodable>(_ path: String) async throws -> T {
        try await send(path, method: "GET", body: Optional<EmptyBody>.none)
    }

    func post<T: Decodable, Body: Encodable>(_ path: String, body: Body) async throws -> T {
        try await send(path, method: "POST", body: body)
    }

    func postEmpty<T: Decodable>(_ path: String) async throws -> T {
        try await send(path, method: "POST", body: Optional<EmptyBody>.none)
    }

    private func send<T: Decodable, Body: Encodable>(_ path: String, method: String, body: Body?) async throws -> T {
        guard let url = URL(string: "\(baseURL)\(path)") else {
            throw APIError.message("接口地址无效")
        }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token, !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.httpBody = try encoder.encode(body)
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw APIError.message("后端无响应")
        }
        guard (200..<300).contains(http.statusCode) else {
            throw APIError.message("HTTP \(http.statusCode)")
        }
        let envelope = try decoder.decode(APIEnvelope<T>.self, from: data)
        guard envelope.code == 200 else {
            throw APIError.message(envelope.msg ?? "请求失败")
        }
        guard let data = envelope.data else {
            throw APIError.message("接口未返回数据")
        }
        return data
    }
}

private struct APIEnvelope<T: Decodable>: Decodable {
    let code: Int
    let msg: String?
    let data: T?
}

private struct EmptyBody: Encodable {}

private enum APIError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        switch self {
        case .message(let text): text
        }
    }
}

private enum AppError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        switch self {
        case .message(let text): text
        }
    }
}

private enum Purchasable {
    case membership(MembershipPackage)
    case privatePackage(PrivatePackage)
}

private struct LoginRequest: Encodable {
    let username: String
    let password: String
}

private struct BookingCreateRequest: Encodable {
    let resourceType: String
    let resourceId: Int
    let source: String
    let idempotentKey: String
    let startTime: String?
    let endTime: String?
}

private struct PaymentCreateRequest: Encodable {
    let paymentType: String
    let targetId: Int?
    let amount: Decimal?
}

private struct CheckinConsumeRequest: Encodable {
    let token: String
}

private struct TrainingLogCreateRequest: Encodable {
    let userId: Int
    let bookingOrderId: Int?
    let trainDate: String
    let focusArea: String
    let content: String
    let remark: String?
}

private struct UserSession: Codable, Identifiable {
    let id: Int
    let username: String?
    let realName: String?
    let nickname: String?
    let email: String?
    let role: String?
    let phone: String?
    let avatar: String?
    let balance: Decimal?
    let type: String?
    let status: String?
    let token: String?

    var displayName: String {
        nickname?.nonEmpty ?? realName?.nonEmpty ?? username ?? "会员"
    }

    var isCoach: Bool {
        role == "COACH"
    }

    var roleLabel: String {
        [
            "MEMBER": "会员",
            "COACH": "教练",
            "ADMIN": "管理员",
            "STAFF": "员工"
        ][role ?? ""] ?? (role ?? "-")
    }
}

private struct MemberHome: Codable {
    let balance: Decimal?
    let activeMembershipName: String?
    let remainingPrivateSessions: Int?
    let upcomingBookings: [BookingOrder]?
}

private struct CourseSlot: Codable, Identifiable {
    let id: Int
    let name: String
    let coachName: String?
    let venueName: String?
    let startTime: String?
    let endTime: String?
    let capacity: Int?
    let bookedCount: Int?
    let availableCount: Int?
    let flashSale: Int?
    let price: Decimal?
    let flashSalePrice: Decimal?
    let description: String?

    var priceText: String {
        if flashSale == 1, let flashSalePrice {
            return money(flashSalePrice)
        }
        return money(price)
    }

    var shortScheduleName: String {
        name
            .replacingOccurrences(of: "®", with: "")
            .replacingOccurrences(of: " ", with: "\n")
    }

    var searchText: String {
        [name, coachName, venueName, description]
            .compactMap { $0?.nonEmpty }
            .joined(separator: " ")
    }

    var tintColor: Color {
        let colors = [
            Color(red: 0.94, green: 0.91, blue: 1.00),
            Color(red: 1.00, green: 0.91, blue: 0.94),
            Color(red: 0.90, green: 0.95, blue: 1.00),
            Color(red: 1.00, green: 0.96, blue: 0.84),
            Color(red: 0.90, green: 1.00, blue: 0.90)
        ]
        return colors[abs(id) % colors.count]
    }

    var accentColor: Color {
        let colors = [
            Color(red: 0.42, green: 0.40, blue: 0.92),
            Color(red: 0.94, green: 0.34, blue: 0.53),
            Color(red: 0.12, green: 0.50, blue: 0.92),
            Color(red: 0.96, green: 0.61, blue: 0.12),
            Color(red: 0.18, green: 0.62, blue: 0.24)
        ]
        return colors[abs(id) % colors.count]
    }
}

private struct CoachDashboard: Codable {
    let todayLessons: Int?
    let pendingApprovals: Int?
    let totalStudents: Int?
    let todayCheckins: Int?
}

private struct CoachScheduleItem: Codable, Identifiable {
    let rawId: Int
    let resourceType: String?
    let name: String
    let venueId: Int?
    let venueName: String?
    let startTime: String?
    let endTime: String?
    let capacity: Int?
    let bookedCount: Int?
    let availableCount: Int?
    let status: String?
    let description: String?
    let price: Decimal?

    var id: String {
        "\(resourceType ?? "SCHEDULE")-\(rawId)"
    }

    var isPrivate: Bool {
        resourceType == "PRIVATE_COACH"
    }

    var typeText: String {
        isPrivate ? "私教" : "团课"
    }

    var shortName: String {
        if isPrivate {
            return "私教\n\(timeLabel)"
        }
        return name.replacingOccurrences(of: " ", with: "\n")
    }

    var iconName: String {
        isPrivate ? "figure.strengthtraining.traditional" : "figure.run"
    }

    var statusText: String {
        [
            "PUBLISHED": "已发布",
            "OPEN": "开放",
            "CLOSED": "已关闭"
        ][status ?? ""] ?? (status ?? "-")
    }

    var capacityText: String {
        "\(bookedCount ?? 0)/\(capacity ?? 0)"
    }

    var timeLabel: String {
        DateTools.timeRange(startTime, endTime)
    }

    var tintColor: Color {
        switch resourceType {
        case "PRIVATE_COACH":
            return Color(red: 1.0, green: 0.93, blue: 0.86)
        case "GROUP_COURSE":
            return Color(red: 0.91, green: 0.89, blue: 1.0)
        default:
            return Palette.mainSoft
        }
    }

    var accentColor: Color {
        switch resourceType {
        case "PRIVATE_COACH":
            return Palette.orange
        case "GROUP_COURSE":
            return Color(red: 0.40, green: 0.42, blue: 0.95)
        default:
            return Palette.text
        }
    }

    enum CodingKeys: String, CodingKey {
        case rawId = "id"
        case resourceType
        case name
        case venueId
        case venueName
        case startTime
        case endTime
        case capacity
        case bookedCount
        case availableCount
        case status
        case description
        case price
    }
}

private struct CoachProfile: Codable, Identifiable {
    let id: Int
    let name: String
    let specialization: String?
    let intro: String?
    let hourlyPrice: Decimal?
    let rating: Decimal?
    let packages: [PrivatePackage]?
    let schedules: [PrivateSchedule]?

    var ratingText: String { money(rating) }
}

private struct PrivateSchedule: Codable, Identifiable {
    let id: Int
    let coachId: Int?
    let venueId: Int?
    let startTime: String?
    let endTime: String?
    let capacity: Int?
    let bookedCount: Int?
    let description: String?
    let status: String?

    var availableCount: Int? {
        guard let capacity else { return nil }
        return capacity - (bookedCount ?? 0)
    }
}

private struct Venue: Codable, Identifiable {
    let id: Int
    let name: String
    let location: String?
    let capacity: Int?
    let description: String?
    let status: Int?
    let openTime: String?
    let closeTime: String?
    let image: String?
    let coverImage: String?
    let layoutJson: String?
    let pricePerHour: Decimal?

    var coverImageURL: URL? {
        URL(string: coverImage?.nonEmpty ?? image?.nonEmpty ?? "")
    }
}

private struct BookingOrder: Codable, Identifiable {
    let id: Int
    let orderNo: String?
    let userId: Int?
    let resourceType: String?
    let resourceId: Int?
    let resourceName: String?
    let venueId: Int?
    let coachId: Int?
    let bookingDate: String?
    let startTime: String?
    let endTime: String?
    let amount: Decimal?
    let paymentStatus: String?
    let status: String?
    let source: String?
    let idempotentKey: String?
    let qrToken: String?
    let qrExpireTime: String?
    let checkedInAt: String?
    let remark: String?
    let createdAt: String?
    let updatedAt: String?

    var statusText: String {
        [
            "CREATED": "待确认",
            "PENDING_PAY": "待支付",
            "CONFIRMED": "已确认",
            "CHECKED_IN": "已签到",
            "COMPLETED": "已完成",
            "CANCELLED": "已取消",
            "REFUNDED": "已退款"
        ][status ?? ""] ?? (status ?? "-")
    }

    var paymentStatusText: String {
        [
            "UNPAID": "待支付",
            "PAID": "已支付",
            "CLOSED": "已关闭",
            "REFUNDED": "已退款"
        ][paymentStatus ?? ""] ?? (paymentStatus ?? "-")
    }

    var statusColor: Color {
        switch status {
        case "CONFIRMED", "CHECKED_IN": Palette.main
        case "PENDING_PAY", "CREATED": Palette.orange
        default: Palette.muted
        }
    }

    var canPay: Bool {
        !["CANCELLED", "REFUNDED", "CHECKED_IN", "COMPLETED"].contains(status ?? "")
            && (paymentStatus == "UNPAID" || status == "PENDING_PAY")
    }

    var canCancel: Bool {
        !["CANCELLED", "REFUNDED", "CHECKED_IN", "COMPLETED"].contains(status ?? "")
    }

    var canCheckin: Bool {
        status == "CONFIRMED"
    }
}

private struct PaymentOrder: Codable, Identifiable {
    let id: Int
    let paymentNo: String
    let userId: Int?
    let paymentType: String?
    let targetType: String?
    let targetId: Int?
    let amount: Decimal?
    let status: String?
    let payloadJson: String?
    let paidAt: String?
    let createdAt: String?
    let updatedAt: String?

    var paymentTypeText: String {
        [
            "RECHARGE": "余额充值",
            "MEMBERSHIP": "会籍套餐",
            "PRIVATE_PACKAGE": "私教课包",
            "BOOKING": "预约支付"
        ][paymentType ?? ""] ?? (paymentType ?? "-")
    }

    var statusText: String {
        [
            "UNPAID": "待支付",
            "PAID": "已支付",
            "CLOSED": "已关闭",
            "REFUNDED": "已退款"
        ][status ?? ""] ?? (status ?? "-")
    }
}

private struct MembershipPackage: Codable, Identifiable {
    let id: Int
    let name: String
    let price: Decimal?
    let days: Int?
    let dailyLimit: Int?
    let description: String?
    let status: String?
    let createdAt: String?
}

private struct PrivatePackage: Codable, Identifiable {
    let id: Int
    let coachId: Int?
    let name: String
    let price: Decimal?
    let totalSessions: Int?
    let description: String?
    let status: String?
    let createdAt: String?
}

private struct BookingCreateResponse: Codable {
    let booking: BookingOrder
    let paymentOrder: PaymentOrder?
    let checkinEligible: Bool?
    let message: String?
}

private struct CheckinToken: Codable {
    let bookingId: Int
    let token: String
    let expireTime: String
}

private struct TrainingLog: Codable, Identifiable {
    let id: Int
    let userId: Int?
    let coachId: Int?
    let bookingOrderId: Int?
    let trainDate: String?
    let focusArea: String?
    let content: String?
    let remark: String?
    let createdAt: String?
}

private enum DateTools {
    private static let backendFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return formatter
    }()

    private static let displayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "MM月dd日 HH:mm"
        return formatter
    }()

    private static let backendDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    private static let displayDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "MM月dd日"
        return formatter
    }()

    private static let dayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "MM.dd"
        return formatter
    }()

    private static let weekdayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "E"
        return formatter
    }()

    private static let dayTitleFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_Hans_CN")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy年M月d日"
        return formatter
    }()

    static func parse(_ value: String?) -> Date? {
        guard let value else { return nil }
        return backendFormatter.date(from: value)
    }

    static func backendString(_ date: Date) -> String {
        backendFormatter.string(from: date)
    }

    static func backendDateString(_ date: Date) -> String {
        backendDateFormatter.string(from: date)
    }

    static func display(_ value: String?) -> String {
        guard let date = parse(value) else { return value ?? "-" }
        return displayFormatter.string(from: date)
    }

    static func displayDate(_ value: String?) -> String {
        guard let value else { return "-" }
        if let date = backendDateFormatter.date(from: value) {
            return displayDateFormatter.string(from: date)
        }
        return display(value)
    }

    static func isToday(_ value: String?) -> Bool {
        guard let date = parse(value) else { return false }
        return Calendar.current.isDateInToday(date)
    }

    static func display(_ date: Date) -> String {
        displayFormatter.string(from: date)
    }

    static func dayLabel(_ date: Date) -> String {
        dayFormatter.string(from: date)
    }

    static func weekdayLabel(_ date: Date) -> String {
        if Calendar.current.isDateInToday(date) { return "今天" }
        if Calendar.current.isDateInTomorrow(date) { return "明天" }
        return weekdayFormatter.string(from: date)
    }

    static func dayTitle(_ date: Date) -> String {
        dayTitleFormatter.string(from: date)
    }

    static func startOfWeek(_ date: Date) -> Date {
        let calendar = Calendar.current
        let start = startOfDay(date)
        let weekday = calendar.component(.weekday, from: start)
        let daysFromMonday = (weekday + 5) % 7
        return calendar.date(byAdding: .day, value: -daysFromMonday, to: start) ?? start
    }

    static func addWeeks(_ count: Int, to date: Date) -> Date {
        Calendar.current.date(byAdding: .day, value: count * 7, to: date) ?? date
    }

    static func weekNumber(_ date: Date) -> Int {
        Calendar.current.component(.weekOfYear, from: date)
    }

    static func weekRangeTitle(_ date: Date) -> String {
        let start = startOfWeek(date)
        let end = Calendar.current.date(byAdding: .day, value: 6, to: start) ?? start
        return "\(backendDateFormatter.string(from: start)) - \(backendDateFormatter.string(from: end))"
    }

    static func monthLabel(_ date: Date) -> String {
        "\(Calendar.current.component(.month, from: date))\n月"
    }

    static func weekdaySymbol(_ date: Date) -> String {
        let symbols = ["日", "一", "二", "三", "四", "五", "六"]
        let index = Calendar.current.component(.weekday, from: date) - 1
        return symbols[min(max(index, 0), symbols.count - 1)]
    }

    static func dayNumber(_ date: Date) -> String {
        "\(Calendar.current.component(.day, from: date))"
    }

    static func range(_ start: String?, _ end: String?) -> String {
        "\(display(start)) - \(display(end).suffix(5))"
    }

    static func timeRange(_ start: String?, _ end: String?) -> String {
        "\(display(start).suffix(5)) - \(display(end).suffix(5))"
    }

    static func startOfDay(_ date: Date) -> Date {
        Calendar.current.startOfDay(for: date)
    }

    static func weekDates(from date: Date = Date()) -> [Date] {
        let base = startOfDay(date)
        return (0..<7).compactMap { Calendar.current.date(byAdding: .day, value: $0, to: base) }
    }

    static func isSameDay(_ value: String?, as date: Date) -> Bool {
        guard let parsed = parse(value) else { return false }
        return Calendar.current.isDate(parsed, inSameDayAs: date)
    }

    static func nextHour() -> Date {
        let date = Date().addingTimeInterval(3600)
        let calendar = Calendar.current
        let parts = calendar.dateComponents([.year, .month, .day, .hour], from: date)
        return calendar.date(from: parts) ?? date
    }

    static func nextSlot(on date: Date) -> Date {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            return nextHour()
        }
        var parts = calendar.dateComponents([.year, .month, .day], from: date)
        parts.hour = 6
        parts.minute = 0
        parts.second = 0
        return calendar.date(from: parts) ?? date
    }

    static func clock(_ date: Date) -> String {
        let parts = Calendar.current.dateComponents([.hour, .minute, .second], from: date)
        return String(format: "%02d:%02d:%02d", parts.hour ?? 0, parts.minute ?? 0, parts.second ?? 0)
    }
}

private func money(_ value: Decimal?) -> String {
    guard let value else { return "0" }
    let number = NSDecimalNumber(decimal: value)
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 2
    return formatter.string(from: number) ?? "\(number)"
}

private extension String {
    var nonEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }

    func containsAny(_ values: [String]) -> Bool {
        values.contains { localizedCaseInsensitiveContains($0) }
    }

    func scheduleBlockText(maxCharsPerLine: Int = 3, maxLines: Int = 7) -> String {
        let cleaned = replacingOccurrences(of: "®", with: "")
            .replacingOccurrences(of: "·", with: "")
            .replacingOccurrences(of: "|", with: "")
            .replacingOccurrences(of: "-", with: "")
            .replacingOccurrences(of: " ", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty else { return self }
        var lines: [String] = []
        var current = ""
        for character in cleaned {
            current.append(character)
            if current.count >= maxCharsPerLine {
                lines.append(current)
                current = ""
            }
            if lines.count == maxLines { break }
        }
        if !current.isEmpty, lines.count < maxLines {
            lines.append(current)
        }
        return lines.joined(separator: "\n")
    }
}

private struct QRCodeImage: View {
    let text: String
    private let context = CIContext()
    private let filter = CIFilter.qrCodeGenerator()

    var body: some View {
        if let image = makeImage() {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
        } else {
            Image(systemName: "qrcode")
                .font(.largeTitle)
        }
    }

    private func makeImage() -> UIImage? {
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let scaled = output.transformed(by: CGAffineTransform(scaleX: 12, y: 12))
        guard let cgImage = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}

private struct QRScannerView: UIViewControllerRepresentable {
    let onToken: (String) -> Void

    func makeUIViewController(context: Context) -> ScannerViewController {
        ScannerViewController(onToken: onToken)
    }

    func updateUIViewController(_ uiViewController: ScannerViewController, context: Context) {}
}

private final class ScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    private let onToken: (String) -> Void
    private let session = AVCaptureSession()
    private var didEmit = false

    init(onToken: @escaping (String) -> Void) {
        self.onToken = onToken
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemGray6
        configure()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        session.stopRunning()
    }

    private func configure() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else {
            showFallback("当前环境没有可用摄像头，可直接粘贴签到 token")
            return
        }

        session.addInput(input)
        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else {
            showFallback("无法初始化扫码输出")
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.qr]

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        preview.frame = view.bounds
        view.layer.addSublayer(preview)

        DispatchQueue.global(qos: .userInitiated).async { [session] in
            session.startRunning()
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        view.layer.sublayers?.compactMap { $0 as? AVCaptureVideoPreviewLayer }.forEach { $0.frame = view.bounds }
    }

    private func showFallback(_ message: String) {
        let label = UILabel()
        label.text = message
        label.numberOfLines = 0
        label.textAlignment = .center
        label.textColor = UIColor.darkGray
        label.font = .systemFont(ofSize: 16, weight: .medium)
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            label.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard !didEmit,
              let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let value = object.stringValue,
              !value.isEmpty else { return }
        didEmit = true
        onToken(value)
    }
}
