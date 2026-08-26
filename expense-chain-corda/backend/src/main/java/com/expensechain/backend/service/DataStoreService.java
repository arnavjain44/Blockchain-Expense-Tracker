package com.expensechain.backend.service;

import com.expensechain.backend.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DataStoreService {

    private static final Logger log = LoggerFactory.getLogger(DataStoreService.class);

    // Standard Corda Node Legal Identities available in the network
    public static final String[] CORDA_NODE_X500 = {
            "O=Garvit,L=New Delhi,C=IN",
            "O=Arnav,L=Mumbai,C=IN",
            "O=Mridul,L=Bengaluru,C=IN"
    };

    public static final String[] CORDA_NODE_NAMES = {
            "Garvit",
            "Arnav",
            "Mridul"
    };

    /**
     * Encapsulates an isolated data partition (Main App vs Demo Mode)
     */
    public static class StoreState {
        public final AtomicLong userSeq = new AtomicLong(1);
        public final AtomicLong groupSeq = new AtomicLong(1);
        public final AtomicLong memberSeq = new AtomicLong(1);
        public final AtomicLong expenseSeq = new AtomicLong(1);
        public final AtomicLong splitSeq = new AtomicLong(1);
        public final AtomicLong settlementSeq = new AtomicLong(1);

        public final Map<Long, User> users = new ConcurrentHashMap<>();
        public final Map<Long, Group> groups = new ConcurrentHashMap<>();
        public final List<GroupMember> groupMembers = Collections.synchronizedList(new ArrayList<>());
        public final List<Expense> expenses = Collections.synchronizedList(new ArrayList<>());
        public final List<ExpenseSplit> expenseSplits = Collections.synchronizedList(new ArrayList<>());
        public final List<Settlement> settlements = Collections.synchronizedList(new ArrayList<>());

        public void clear() {
            userSeq.set(1);
            groupSeq.set(1);
            memberSeq.set(1);
            expenseSeq.set(1);
            splitSeq.set(1);
            settlementSeq.set(1);

            users.clear();
            groups.clear();
            groupMembers.clear();
            expenses.clear();
            expenseSplits.clear();
            settlements.clear();
        }
    }

    private final StoreState mainStore = new StoreState();
    private final StoreState demoStore = new StoreState();

    @PostConstruct
    public void init() {
        // Main store starts completely clean
        mainStore.clear();
        // Seed initial fresh randomized demo data for Demo Mode
        resetDemoStore();
        log.info("DataStoreService initialized: Main Store is clean; Demo Store seeded with fresh randomized data.");
    }

    private StoreState getStore(boolean isDemo) {
        return isDemo ? demoStore : mainStore;
    }

    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // DEV RESET & DEMO SEED GENERATOR
    // =========================================================================

    public synchronized void resetDevEnvironment(boolean seedDemo) {
        mainStore.clear();
        demoStore.clear();
        if (seedDemo) {
            resetDemoStore();
        }
        log.info("Development environment reset completed. SeedDemo: {}", seedDemo);
    }

    public synchronized void resetDemoStore() {
        demoStore.clear();
        generateRandomizedDemoData();
    }

    private void generateRandomizedDemoData() {
        Random rand = new Random();

        // 1. Large Pool of 55+ Fictional User Names
        List<String[]> candidateUsers = Arrays.asList(
                new String[]{"Priya Sharma", "priya.s@demo.com", "9876543210"},
                new String[]{"Rohan Mehta", "rohan.m@demo.com", "9876543211"},
                new String[]{"Aisha Khan", "aisha.k@demo.com", "9876543212"},
                new String[]{"Vikram Rao", "vikram.r@demo.com", "9876543213"},
                new String[]{"Ananya Iyer", "ananya.i@demo.com", "9876543214"},
                new String[]{"Kavya Nair", "kavya.n@demo.com", "9876543215"},
                new String[]{"Aditya Joshi", "aditya.j@demo.com", "9876543216"},
                new String[]{"Maya Chen", "maya.c@demo.com", "9876543217"},
                new String[]{"Leo Patel", "leo.p@demo.com", "9876543218"},
                new String[]{"Samira Roy", "samira.r@demo.com", "9876543219"},
                new String[]{"Alex Rivera", "alex.r@demo.com", "9876543220"},
                new String[]{"Daniel Kim", "daniel.k@demo.com", "9876543221"},
                new String[]{"Elena Rostova", "elena.r@demo.com", "9876543222"},
                new String[]{"Sanjay Verma", "sanjay.v@demo.com", "9876543223"},
                new String[]{"Tara Deshmukh", "tara.d@demo.com", "9876543224"},
                new String[]{"Carlos Silva", "carlos.s@demo.com", "9876543225"},
                new String[]{"Naomi Osaka", "naomi.o@demo.com", "9876543226"},
                new String[]{"Lucas Meyer", "lucas.m@demo.com", "9876543227"},
                new String[]{"Fatima Al-Mansoor", "fatima.m@demo.com", "9876543228"},
                new String[]{"Hiroshi Tanaka", "hiroshi.t@demo.com", "9876543229"},
                new String[]{"Chloe Dubois", "chloe.d@demo.com", "9876543230"},
                new String[]{"Marcus Vance", "marcus.v@demo.com", "9876543231"},
                new String[]{"Divya Pillai", "divya.p@demo.com", "9876543232"},
                new String[]{"Liam O'Connor", "liam.o@demo.com", "9876543233"},
                new String[]{"Nina Petrova", "nina.p@demo.com", "9876543234"},
                new String[]{"Omar Farooq", "omar.f@demo.com", "9876543235"},
                new String[]{"Sophie Martin", "sophie.m@demo.com", "9876543236"},
                new String[]{"Rahul Singhania", "rahul.s@demo.com", "9876543237"},
                new String[]{"Zara Qureshi", "zara.q@demo.com", "9876543238"},
                new String[]{"Ethan Walker", "ethan.w@demo.com", "9876543239"},
                new String[]{"Priyanka Das", "priyanka.d@demo.com", "9876543240"},
                new String[]{"Gabriel Santos", "gabriel.s@demo.com", "9876543241"},
                new String[]{"Maya Lin", "maya.l@demo.com", "9876543242"},
                new String[]{"Vikram Seth", "vikram.s@demo.com", "9876543243"},
                new String[]{"Ananya Menon", "ananya.m@demo.com", "9876543244"},
                new String[]{"David Novak", "david.n@demo.com", "9876543245"},
                new String[]{"Meera Kapoor", "meera.k@demo.com", "9876543246"},
                new String[]{"Benjamin Hayes", "benjamin.h@demo.com", "9876543247"},
                new String[]{"Yasmin Becker", "yasmin.b@demo.com", "9876543248"},
                new String[]{"Siddharth Malhotra", "sid.m@demo.com", "9876543249"},
                new String[]{"Clara Oswald", "clara.o@demo.com", "9876543250"},
                new String[]{"Tariq Mansoor", "tariq.m@demo.com", "9876543251"},
                new String[]{"Hannah Abbott", "hannah.a@demo.com", "9876543252"},
                new String[]{"Kunal Shah", "kunal.s@demo.com", "9876543253"},
                new String[]{"Zoe Saldana", "zoe.s@demo.com", "9876543254"},
                new String[]{"Arjun Reddy", "arjun.r@demo.com", "9876543255"},
                new String[]{"Freya Lindqvist", "freya.l@demo.com", "9876543256"},
                new String[]{"Rohan Joshi", "rohan.j@demo.com", "9876543257"},
                new String[]{"Isabella Gomez", "isabella.g@demo.com", "9876543258"},
                new String[]{"Devika Nambiar", "devika.n@demo.com", "9876543259"},
                new String[]{"Julian Ross", "julian.r@demo.com", "9876543260"},
                new String[]{"Sanya Mirza", "sanya.m@demo.com", "9876543261"},
                new String[]{"Felix Weber", "felix.w@demo.com", "9876543262"},
                new String[]{"Ritu Agarwal", "ritu.a@demo.com", "9876543263"},
                new String[]{"Tanvi Deshmukh", "tanvi.d@demo.com", "9876543264"}
        );

        List<String[]> shuffledUsers = new ArrayList<>(candidateUsers);
        Collections.shuffle(shuffledUsers, rand);
        // Randomly choose 5 to 20 users
        int userCount = 5 + rand.nextInt(16);

        String[] cities = {"New Delhi", "Mumbai", "Bengaluru", "Hyderabad", "Pune", "Chennai", "Kolkata"};

        List<User> demoUsers = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            String[] uData = shuffledUsers.get(i);
            String city = cities[i % cities.length];
            String x500 = "O=" + uData[0] + ",L=" + city + ",C=IN";
            User u = registerUser(true, uData[0], uData[1], "password", uData[2], x500);
            demoUsers.add(u);
        }

        // 2. Large Pool of 55+ Fictional Groups
        List<String[]> candidateGroups = Arrays.asList(
                new String[]{"Manali Roadtrip", "Shared travel, stay, and food expenses for mountain adventure"},
                new String[]{"Flat 304 Residency", "Monthly apartment expenses, groceries, and utility bills"},
                new String[]{"Hackathon Project Alpha", "Cloud infrastructure, domain registrations, and team meals"},
                new String[]{"Goa Weekend Trip", "Beach resort, water sports, and sunset cafe dinners"},
                new String[]{"Apartment 4B Roommates", "Electricity, high-speed fiber broadband, and cleaning supplies"},
                new String[]{"Diwali Celebration Crew", "Sweets, festive decorations, and banquet feast"},
                new String[]{"EuroTrip Backpackers", "Hostels, eurail train passes, and museum entries"},
                new String[]{"Bangalore Tech House", "Shared co-living house expenses and streaming recharges"},
                new String[]{"Weekend Football League", "Turf booking, kit uniforms, and sports drinks"},
                new String[]{"Office Lunch Club", "Daily shared team lunches and coffee breaks"},
                new String[]{"Campus Study Circle", "Textbooks, lab kits, and late-night snacks"},
                new String[]{"Beach Resort Getaway", "Private villa stay, swimming pool, and barbecue night"},
                new String[]{"Roadtrip Leh-Ladakh", "Bike rentals, backup fuel, and mountain camps"},
                new String[]{"Gym & Fitness Buddies", "Group trainer fee, protein supplements, and healthy meals"},
                new String[]{"Wedding Sangeet Troupe", "Costume rentals, sound system, and choreography"},
                new String[]{"Music Festival Tour", "Festival passes, camping tents, and food trucks"},
                new String[]{"Cloud Lab Infrastructure", "AWS compute credits, API subscriptions, and domains"},
                new String[]{"Book Club & Cafe Meet", "Coffee tasting, novel orders, and bakery snacks"},
                new String[]{"New Year Mountain Cabin", "Cabin booking, firewood, and fondue dinner"},
                new String[]{"Mountain Biking Trail", "Trail permits, bicycle servicing, and energy bars"},
                new String[]{"Summer Camp Adventure", "Kayaking, trekking gear, and guide charges"},
                new String[]{"Project Sprint Beta", "Design licenses, Figma team plan, and team dinner"},
                new String[]{"Tennis Club Sundays", "Clay court rental, tennis balls, and refreshments"},
                new String[]{"Baking & Pastry Workshop", "Baking supplies, ovens, and artisan chocolate"},
                new String[]{"Gaming Den & Lan Party", "Server rentals, pizza orders, and energy drinks"},
                new String[]{"Board Games Night", "New board game purchases, artisanal sodas, and nachos"},
                new String[]{"Tokyo Urban Explorer", "Subway cards, ramen shops, and city tours"},
                new String[]{"Paris Sightseeing Tour", "Hotel reservation, bistro meals, and river cruise"},
                new String[]{"Startup Incubator Hub", "Coworking desks, coffee beans, and printer cartridges"},
                new String[]{"Photography Photo-Walk", "Studio lighting, memory cards, and chai breaks"},
                new String[]{"Desert Safari Camping", "Dune bashing, desert camp tents, and cultural dinner"},
                new String[]{"Island Cruise & Snorkel", "Boat charter, snorkeling equipment, and seafood feast"},
                new String[]{"Rooftop Barbecue Night", "Grill charcoal, skewers, sauces, and drinks"},
                new String[]{"Italian Cooking Club", "Pasta ingredients, cheese wheel, and olive oils"},
                new String[]{"Coworking Space Commons", "Shared hot desks, conference room, and tea machine"},
                new String[]{"Coding Bootcamp Study", "Course textbooks, video subscriptions, and snacks"},
                new String[]{"Film Screening Society", "Projector rental, pop-corn maker, and indie film licenses"},
                new String[]{"Yoga & Wellness Retreat", "Meditation hall booking, organic meals, and mats"},
                new String[]{"Skiing & Snowboard Trip", "Ski lift passes, ski jacket rentals, and hot cocoa"},
                new String[]{"Art Studio Collective", "Canvases, oil paints, easels, and brushes"},
                new String[]{"Charity Gala Organizers", "Venue booking, flyers, and sound equipment"},
                new String[]{"Gourmet Dinner Club", "Multi-course tasting menus and dessert platters"},
                new String[]{"Hackathon Team Omega", "Cloud databases, GPUs, and midnight energy drinks"},
                new String[]{"Flat 502 Penthouse", "Rent split, maintenance, and fiber optic connection"},
                new String[]{"Lakehouse Summer Stay", "Lakeside cottage, fishing rods, and groceries"},
                new String[]{"Rann of Kutch Roadtrip", "Tent city booking, white desert entry, and handicrafts"},
                new String[]{"Graduation Celebration", "Dinner party, graduation gifts, and photographer"},
                new String[]{"Spiti Valley Expedition", "4x4 vehicle rental, homestays, and oxygen cylinders"},
                new String[]{"Formula 1 Watch Party", "Sports channel pass, snacks, and team merchandise"},
                new String[]{"Robotics Workshop Team", "Microcontrollers, sensors, and 3D printing filaments"},
                new String[]{"Trekking & Forest Camp", "Sleeping bags, camp stoves, and water purifiers"},
                new String[]{"Jazz Evening Lounge", "Club entry, live jazz tickets, and drinks"},
                new String[]{"Potluck & Culinary Guild", "Exotic spices, ingredients, and tableware"},
                new String[]{"Badminton Doubles Club", "Indoor court booking, shuttlecocks, and grips"},
                new String[]{"Astronomy Star-Gazing", "Telescope hire, campsite permits, and thermos flasks"}
        );

        List<String[]> shuffledGroups = new ArrayList<>(candidateGroups);
        Collections.shuffle(shuffledGroups, rand);
        int groupCount = Math.min(shuffledGroups.size(), Math.max(3, Math.min(demoUsers.size(), 4 + rand.nextInt(4))));

        List<Group> demoGroups = new ArrayList<>();
        for (int g = 0; g < groupCount; g++) {
            String[] gData = shuffledGroups.get(g);
            User creator = demoUsers.get(g % demoUsers.size());
            Group grp = createGroup(true, gData[0], gData[1], creator.getId());
            demoGroups.add(grp);

            // Add a subset (3 to 8 members) to this group
            List<User> shuffledGroupUsers = new ArrayList<>(demoUsers);
            Collections.shuffle(shuffledGroupUsers, rand);
            int groupMemberCount = Math.min(shuffledGroupUsers.size(), Math.max(3, 3 + rand.nextInt(6)));
            
            // Ensure creator is in group
            addMember(true, grp.getId(), creator.getId(), "ADMIN");
            for (int uIdx = 0; uIdx < groupMemberCount; uIdx++) {
                User u = shuffledGroupUsers.get(uIdx);
                if (!u.getId().equals(creator.getId())) {
                    addMember(true, grp.getId(), u.getId(), "MEMBER");
                }
            }
        }

        // 3. Realistic Expense Templates
        String[][] expenseTemplates = {
                {"Riverside Resort Stay", "ACCOMMODATION", "4200.00", "3 nights mountain view stay with breakfast"},
                {"Highway Toll & Fuel", "TRAVEL", "1800.00", "Fuel refill and fastag expressway charges"},
                {"Team Dinner & Barbecue", "FOOD", "2400.00", "Evening dining at local bistro"},
                {"High-Speed WiFi Bill", "UTILITIES", "999.00", "Fiber broadband monthly recharge"},
                {"Supermarket Groceries", "FOOD", "1650.00", "Weekly essentials, fruits, and dairy"},
                {"Adventure Rafting Pass", "ENTERTAINMENT", "3000.00", "River rafting and safety equipment rental"},
                {"Cloud Server Hosting", "UTILITIES", "1500.00", "AWS and database compute instances"},
                {"Late Night Cafe & Snacks", "FOOD", "750.00", "Coffee, pizzas, and study snacks"},
                {"Airport Taxi Ride", "TRAVEL", "1200.00", "Cab from terminal to hotel"}
        };

        LocalDate today = LocalDate.now();

        // Seed 4-6 expenses per group
        for (Group grp : demoGroups) {
            List<GroupMember> members = getGroupMembers(true, grp.getId());
            if (members.isEmpty()) continue;

            int expCount = 4 + rand.nextInt(3); // 4 to 6 expenses
            for (int e = 0; e < expCount; e++) {
                String[] template = expenseTemplates[(grp.getId().intValue() * 3 + e) % expenseTemplates.length];
                String title = template[0];
                String category = template[1];
                double baseAmt = Double.parseDouble(template[2]);
                // Vary amount slightly for realism
                double amount = Math.round((baseAmt * (0.85 + rand.nextDouble() * 0.35)) * 100.0) / 100.0;
                String desc = template[3];
                String date = today.minusDays(e * 2L + rand.nextInt(2)).toString();

                GroupMember payerMember = members.get(rand.nextInt(members.size()));
                Long paidBy = payerMember.getUserId();

                // Select participants (all or subset)
                List<Long> pUserIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
                if (pUserIds.size() > 2 && rand.nextBoolean()) {
                    Collections.shuffle(pUserIds, rand);
                    pUserIds = pUserIds.subList(0, Math.max(2, pUserIds.size() - 1));
                }

                String splitType = (e % 3 == 0) ? "CUSTOM" : "EQUAL";
                List<Map<String, Object>> calculatedSplits = new ArrayList<>();
                long totalMinorUnits = Math.round(amount * 100.0);

                if ("EQUAL".equalsIgnoreCase(splitType)) {
                    int n = pUserIds.size();
                    long shareMinor = totalMinorUnits / n;
                    long remainder = totalMinorUnits - (shareMinor * n);
                    for (int i = 0; i < n; i++) {
                        long sMinor = shareMinor + (i == n - 1 ? remainder : 0);
                        Map<String, Object> split = new HashMap<>();
                        split.put("userId", pUserIds.get(i));
                        split.put("shareAmount", sMinor / 100.0);
                        calculatedSplits.add(split);
                    }
                } else {
                    // Custom split with mathematically exact partition
                    int n = pUserIds.size();
                    long remainingMinor = totalMinorUnits;
                    for (int i = 0; i < n; i++) {
                        long sMinor;
                        if (i == n - 1) {
                            sMinor = remainingMinor;
                        } else {
                            long approx = Math.round((totalMinorUnits * (1.0 / n)) * (0.7 + rand.nextDouble() * 0.6));
                            sMinor = Math.min(approx, remainingMinor - (n - i - 1) * 100);
                            if (sMinor <= 0) sMinor = 100;
                            remainingMinor -= sMinor;
                        }
                        Map<String, Object> split = new HashMap<>();
                        split.put("userId", pUserIds.get(i));
                        split.put("shareAmount", sMinor / 100.0);
                        calculatedSplits.add(split);
                    }
                }

                String cordaTxId = "0x" + sha256(grp.getId() + "|" + title + "|" + amount + "|" + date).substring(0, 40).toUpperCase();

                saveExpense(true, grp.getId(), title, amount, category, desc, date, paidBy, splitType, cordaTxId, calculatedSplits);
            }

            // Seed 1 partial settlement if simplified debts exist
            List<Map<String, Object>> debts = simplifyDebts(true, grp.getId());
            if (!debts.isEmpty()) {
                Map<String, Object> firstDebt = debts.get(0);
                Long from = (Long) firstDebt.get("from");
                Long to = (Long) firstDebt.get("to");
                double debtAmt = ((Number) firstDebt.get("amount")).doubleValue();
                // Partially or fully settle
                double settleAmt = Math.round((debtAmt * 0.5) * 100.0) / 100.0;
                if (settleAmt >= 10.0) {
                    String settleTx = "0x" + sha256("SETTLE|" + grp.getId() + "|" + from + "|" + to + "|" + settleAmt).substring(0, 40).toUpperCase();
                    saveSettlement(true, grp.getId(), from, to, settleAmt, settleTx);
                }
            }
        }
    }

    // =========================================================================
    // USER METHODS
    // =========================================================================

    public User registerUser(boolean isDemo, String name, String email, String password, String phone, String x500) {
        StoreState store = getStore(isDemo);
        String cleanEmail = email.trim().toLowerCase();
        for (User u : store.users.values()) {
            if (u.getEmail().equalsIgnoreCase(cleanEmail)) {
                throw new IllegalArgumentException("Email already registered: " + cleanEmail);
            }
        }
        Long id = store.userSeq.getAndIncrement();
        if (x500 == null || x500.isEmpty()) {
            int nodeIdx = (int) (id % CORDA_NODE_X500.length);
            x500 = CORDA_NODE_X500[nodeIdx];
        }
        User user = new User(id, name, cleanEmail, sha256(password), phone, x500, Instant.now().toString());
        store.users.put(id, user);
        return user;
    }

    public User authenticate(boolean isDemo, String email, String password) {
        StoreState store = getStore(isDemo);
        String cleanEmail = email.trim().toLowerCase();
        String hash = sha256(password);
        return store.users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(cleanEmail) && u.getPasswordHash().equals(hash))
                .findFirst()
                .orElse(null);
    }

    public User getUserById(boolean isDemo, Long id) {
        return getStore(isDemo).users.get(id);
    }

    public List<User> getAllUsers(boolean isDemo) {
        return new ArrayList<>(getStore(isDemo).users.values());
    }

    // =========================================================================
    // GROUP METHODS
    // =========================================================================

    public Group createGroup(boolean isDemo, String name, String description, Long createdBy) {
        StoreState store = getStore(isDemo);
        Long id = store.groupSeq.getAndIncrement();
        Group group = new Group(id, name, description, createdBy, Instant.now().toString());
        store.groups.put(id, group);
        return group;
    }

    public Group getGroup(boolean isDemo, Long id) {
        return getStore(isDemo).groups.get(id);
    }

    public List<Group> getAllGroups(boolean isDemo) {
        return new ArrayList<>(getStore(isDemo).groups.values());
    }

    public List<Group> getGroupsForUser(boolean isDemo, Long userId) {
        StoreState store = getStore(isDemo);
        Set<Long> userGroupIds = store.groupMembers.stream()
                .filter(m -> m.getUserId().equals(userId))
                .map(GroupMember::getGroupId)
                .collect(Collectors.toSet());
        return store.groups.values().stream()
                .filter(g -> userGroupIds.contains(g.getId()))
                .collect(Collectors.toList());
    }

    public GroupMember addMember(boolean isDemo, Long groupId, Long userId, String role) {
        StoreState store = getStore(isDemo);
        boolean exists = store.groupMembers.stream()
                .anyMatch(m -> m.getGroupId().equals(groupId) && m.getUserId().equals(userId));
        if (exists) {
            throw new IllegalArgumentException("User is already a member of this group");
        }
        Long id = store.memberSeq.getAndIncrement();
        GroupMember member = new GroupMember(id, groupId, userId, role, Instant.now().toString());
        store.groupMembers.add(member);
        return member;
    }

    public List<GroupMember> getGroupMembers(boolean isDemo, Long groupId) {
        return getStore(isDemo).groupMembers.stream()
                .filter(m -> m.getGroupId().equals(groupId))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // EXPENSE METHODS
    // =========================================================================

    public Expense saveExpense(boolean isDemo, Long groupId, String title, double amount, String category,
                               String description, String expenseDate, Long paidBy,
                               String splitType, String cordaTxId,
                               List<Map<String, Object>> splits) {
        StoreState store = getStore(isDemo);
        Long id = store.expenseSeq.getAndIncrement();
        Expense expense = new Expense(id, groupId, title, amount, category, description,
                expenseDate, paidBy, splitType, cordaTxId, Instant.now().toString());
        store.expenses.add(expense);

        for (Map<String, Object> s : splits) {
            Long uid = ((Number) s.get("userId")).longValue();
            double share = ((Number) s.get("shareAmount")).doubleValue();
            store.expenseSplits.add(new ExpenseSplit(store.splitSeq.getAndIncrement(), id, uid, share));
        }

        return expense;
    }

    public List<Expense> getExpensesForGroup(boolean isDemo, Long groupId) {
        return getStore(isDemo).expenses.stream()
                .filter(e -> e.getGroupId().equals(groupId))
                .sorted((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate()))
                .collect(Collectors.toList());
    }

    public List<ExpenseSplit> getSplitsForExpense(boolean isDemo, Long expenseId) {
        return getStore(isDemo).expenseSplits.stream()
                .filter(s -> s.getExpenseId().equals(expenseId))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // SETTLEMENT METHODS
    // =========================================================================

    public Settlement saveSettlement(boolean isDemo, Long groupId, Long paidBy, Long paidTo, double amount, String cordaTxId) {
        StoreState store = getStore(isDemo);
        Long id = store.settlementSeq.getAndIncrement();
        String now = Instant.now().toString();
        Settlement settlement = new Settlement(id, groupId, paidBy, paidTo, amount, "CONFIRMED_ON_CORDA", cordaTxId, now, now);
        store.settlements.add(settlement);
        return settlement;
    }

    public Settlement savePendingSettlement(boolean isDemo, Long groupId, Long paidBy, Long paidTo, double amount) {
        StoreState store = getStore(isDemo);
        Long id = store.settlementSeq.getAndIncrement();
        String now = Instant.now().toString();
        Settlement settlement = new Settlement(id, groupId, paidBy, paidTo, amount, "PENDING_VERIFICATION", null, now, null);
        store.settlements.add(settlement);
        return settlement;
    }

    public Settlement getSettlementById(boolean isDemo, Long id) {
        return getStore(isDemo).settlements.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Settlement> getPendingSettlementsForUser(boolean isDemo, Long userId) {
        return getStore(isDemo).settlements.stream()
                .filter(s -> (s.getPaidTo().equals(userId) && "PENDING_VERIFICATION".equalsIgnoreCase(s.getStatus()))
                        || (s.getPaidBy().equals(userId) && "REJECTED".equalsIgnoreCase(s.getStatus())))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Settlement dismissSettlementRejection(boolean isDemo, Long id) {
        Settlement s = getSettlementById(isDemo, id);
        if (s != null && "REJECTED".equalsIgnoreCase(s.getStatus())) {
            s.setStatus("REJECTED_DISMISSED");
        }
        return s;
    }

    public boolean hasPendingSettlement(boolean isDemo, Long groupId, Long paidBy, Long paidTo) {
        return getStore(isDemo).settlements.stream()
                .anyMatch(s -> s.getGroupId().equals(groupId)
                        && s.getPaidBy().equals(paidBy)
                        && s.getPaidTo().equals(paidTo)
                        && "PENDING_VERIFICATION".equalsIgnoreCase(s.getStatus()));
    }

    public Settlement updateSettlementStatus(boolean isDemo, Long id, String status, String cordaTxId) {
        Settlement s = getSettlementById(isDemo, id);
        if (s != null) {
            s.setStatus(status);
            if (cordaTxId != null) s.setCordaTxId(cordaTxId);
            s.setSettledAt(Instant.now().toString());
        }
        return s;
    }

    public List<Settlement> getSettlementsForGroup(boolean isDemo, Long groupId) {
        return getStore(isDemo).settlements.stream()
                .filter(s -> s.getGroupId().equals(groupId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // BALANCE & DEBT SIMPLIFICATION
    // =========================================================================

    public Map<Long, Double> calculateNetBalances(boolean isDemo, Long groupId) {
        Map<Long, Double> net = new HashMap<>();

        for (Expense e : getExpensesForGroup(isDemo, groupId)) {
            net.put(e.getPaidBy(), net.getOrDefault(e.getPaidBy(), 0.0) + e.getAmount());
            for (ExpenseSplit s : getSplitsForExpense(isDemo, e.getId())) {
                net.put(s.getUserId(), net.getOrDefault(s.getUserId(), 0.0) - s.getShareAmount());
            }
        }

        for (Settlement s : getSettlementsForGroup(isDemo, groupId)) {
            if ("COMPLETED".equalsIgnoreCase(s.getStatus()) || "CONFIRMED_ON_CORDA".equalsIgnoreCase(s.getStatus())) {
                net.put(s.getPaidBy(), net.getOrDefault(s.getPaidBy(), 0.0) + s.getAmount());
                net.put(s.getPaidTo(), net.getOrDefault(s.getPaidTo(), 0.0) - s.getAmount());
            }
        }

        for (Map.Entry<Long, Double> entry : net.entrySet()) {
            entry.setValue(Math.round(entry.getValue() * 100.0) / 100.0);
        }
        return net;
    }

    public List<Map<String, Object>> simplifyDebts(boolean isDemo, Long groupId) {
        Map<Long, Double> net = calculateNetBalances(isDemo, groupId);
        List<Map.Entry<Long, Double>> creditors = new ArrayList<>();
        List<Map.Entry<Long, Double>> debtors = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : net.entrySet()) {
            if (entry.getValue() > 0.01) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            } else if (entry.getValue() < -0.01) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), -entry.getValue()));
            }
        }

        creditors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        debtors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Map<String, Object>> result = new ArrayList<>();
        int ci = 0, di = 0;
        while (ci < creditors.size() && di < debtors.size()) {
            Map.Entry<Long, Double> c = creditors.get(ci);
            Map.Entry<Long, Double> d = debtors.get(di);
            double settle = Math.round(Math.min(c.getValue(), d.getValue()) * 100.0) / 100.0;

            if (settle > 0.01) {
                Map<String, Object> item = new HashMap<>();
                item.put("from", d.getKey());
                item.put("to", c.getKey());
                item.put("amount", settle);
                item.put("pendingVerification", hasPendingSettlement(isDemo, groupId, d.getKey(), c.getKey()));
                result.add(item);
            }

            c.setValue(Math.round((c.getValue() - settle) * 100.0) / 100.0);
            d.setValue(Math.round((d.getValue() - settle) * 100.0) / 100.0);

            if (c.getValue() <= 0.01) ci++;
            if (d.getValue() <= 0.01) di++;
        }

        return result;
    }
}
