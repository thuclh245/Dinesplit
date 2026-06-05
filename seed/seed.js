const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const auth = admin.auth();

const SEED_VERSION = 5;
const now = Date.now();

// 1. DATASET: 20 USERS (CORE, NORMAL, EDGE CASES, AND GROUP USERS)
const users = [
  {
    uid: "demo_user_01",
    displayName: "Minh Tú",
    username: "minhtu_foodie",
    email: "minhtu.demo@example.com",
    avatarUrl: "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop&q=60",
    bio: "Đam mê khám phá quán ăn vỉa hè & cà phê không gian làm việc đẹp.",
    diningStyles: ["Street Food", "Cafe Hopper", "Italian"],
    followersCount: 8,
    followingCount: 10,
    postsCount: 5,
    fcmToken: "mock_fcm_token_01",
    followingIds: ["demo_user_02", "demo_user_03", "demo_user_04", "demo_user_05", "demo_user_06", "demo_user_07", "demo_user_08", "demo_user_09"],
    followerIds: ["demo_user_02", "demo_user_04", "demo_user_06", "demo_user_10"],
    searchKeywords: ["minh", "tu", "minhtu", "foodie", "street", "cafe", "hopper"]
  },
  {
    uid: "demo_user_02",
    displayName: "An Rivera",
    username: "arivera",
    email: "arivera.demo@example.com",
    avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=60",
    bio: "Pizza, Pasta, Fine Dining. Ăn ngon là liều thuốc hạnh phúc.",
    diningStyles: ["Fine Dining", "Italian", "Korean"],
    followersCount: 5,
    followingCount: 4,
    postsCount: 3,
    fcmToken: "mock_fcm_token_02",
    followingIds: ["demo_user_01", "demo_user_03", "demo_user_10"],
    followerIds: ["demo_user_01", "demo_user_03"],
    searchKeywords: ["an", "rivera", "arivera", "pizza", "pasta", "italian"]
  },
  {
    uid: "demo_user_03",
    displayName: "Khánh Linh",
    username: "khanhlinh_eats",
    email: "khanhlinh.demo@example.com",
    avatarUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=60",
    bio: "Cà phê buổi sáng & Đồ ngọt kiểu Pháp. Nghiện ăn đồ Hàn Quốc siêu cay.",
    diningStyles: ["Cafe Hopper", "Korean", "Dessert"],
    followersCount: 6,
    followingCount: 3,
    postsCount: 3,
    fcmToken: "mock_fcm_token_03",
    followingIds: ["demo_user_01", "demo_user_02"],
    followerIds: ["demo_user_01", "demo_user_02"],
    searchKeywords: ["khanh", "linh", "khanhlinh", "cafe", "dessert", "korean"]
  },
  {
    uid: "demo_user_04",
    displayName: "Nguyễn Hoàng Nam Với Một Cái Tên Rất Dài Để Test Tràn Chữ UI",
    username: "nam_longname",
    email: "nam.demo@example.com",
    avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=60",
    bio: "Không thích nói nhiều, chỉ thích ăn lẩu nướng BBQ Hàn Quốc nguyên đêm.",
    diningStyles: ["BBQ", "Korean"],
    followersCount: 2,
    followingCount: 2,
    postsCount: 2,
    fcmToken: "mock_fcm_token_04",
    followingIds: ["demo_user_01"],
    followerIds: ["demo_user_01"],
    searchKeywords: ["nguyen", "hoang", "nam", "longname", "bbq", "korean"]
  },
  {
    uid: "demo_user_05",
    displayName: "Sơn Chen",
    username: "schen_eats",
    email: "schen.demo@example.com",
    avatarUrl: "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150&auto=format&fit=crop&q=60",
    bio: "Tập luyện đều đặn & Ăn uống Eat Clean khoa học lành mạnh.",
    diningStyles: ["Healthy", "Cafe Hopper"],
    followersCount: 3,
    followingCount: 5,
    postsCount: 2,
    fcmToken: "mock_fcm_token_05",
    followingIds: ["demo_user_01", "demo_user_02"],
    followerIds: ["demo_user_01"],
    searchKeywords: ["son", "chen", "schen", "healthy", "eat", "clean"]
  },
  {
    uid: "demo_user_14",
    displayName: "Trần Vân Anh (Avatar rỗng)",
    username: "vananh_noavatar",
    email: "vananh.demo@example.com",
    avatarUrl: "", // Rỗng để test fallback chữ cái đại diện
    bio: "Street food vỉa hè là chân ái cuộc đời.",
    diningStyles: ["Street Food"],
    followersCount: 1,
    followingCount: 1,
    postsCount: 1,
    fcmToken: "",
    followingIds: ["demo_user_01"],
    followerIds: [],
    searchKeywords: ["tran", "van", "anh", "vananh", "noavatar", "street"]
  },
  {
    uid: "demo_user_15",
    displayName: "Empty Profile User",
    username: "empty_user",
    email: "empty.demo@example.com",
    avatarUrl: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=60",
    bio: "", // Trống để test empty state profile
    diningStyles: [],
    followersCount: 0,
    followingCount: 0,
    postsCount: 0,
    fcmToken: "",
    followingIds: [],
    followerIds: [],
    searchKeywords: ["empty", "user", "profile"]
  },
  {
    uid: "demo_user_16",
    displayName: "Bánh Mì Hà Nội",
    username: "banh_mi_trai_nghiem",
    email: "banhmi.demo@example.com",
    avatarUrl: "https://images.unsplash.com/photo-1600454309261-3dc9b7597637?w=150&auto=format&fit=crop&q=60",
    bio: "Chuyên review các loại bánh mì pate giòn rụm phố cổ.",
    diningStyles: ["Street Food", "Vietnamese"],
    followersCount: 4,
    followingCount: 2,
    postsCount: 2,
    fcmToken: "",
    followingIds: ["demo_user_01"],
    followerIds: ["demo_user_01"],
    searchKeywords: ["banh", "mi", "ha", "noi", "vietnamese", "street"]
  }
];

const workingAvatars = [
  "1535713875002-d1d0cf377fde", // Male 1
  "1494790108377-be9c29b29330", // Female 1
  "1507003211169-0a1dd7228f2d", // Male 2
  "1438761681033-6461ffad8d80", // Female 2
  "1500648767791-00dcc994a43e", // Male 3
  "1534528741775-53994a69daeb", // Female 3
  "1527983359383-4758693f760c", // Male 4
  "1544005313-94ddf0286df2", // Female 4
  "1506794778202-cad84cf45f1d", // Male 5
  "1517841905240-472988babdf9"  // Female 5
];

// Tự động bù thêm các user từ 06 đến 20 để đủ dataset
for (let i = 6; i <= 20; i++) {
  const uid = `demo_user_${String(i).padStart(2, "0")}`;
  if (users.some(u => u.uid === uid)) continue;

  users.push({
    uid,
    displayName: `Demo Foodie ${i}`,
    username: `demo_foodie_${i}`,
    email: `demo_user_${i}@example.com`,
    avatarUrl: i % 3 === 0 ? "" : `https://images.unsplash.com/photo-${workingAvatars[i % workingAvatars.length]}?w=150&auto=format&fit=crop&q=60`,
    bio: i % 4 === 0 ? "" : `Thành viên đam mê ăn uống số ${i} trong DineSplit.`,
    diningStyles: i % 2 === 0 ? ["Street Food", "Vietnamese"] : ["Cafe Hopper", "Dessert"],
    followersCount: i <= 10 ? 2 : 0,
    followingCount: 3,
    postsCount: i % 3,
    fcmToken: "",
    followingIds: ["demo_user_01", "demo_user_02"],
    followerIds: i <= 10 ? ["demo_user_01"] : [],
    searchKeywords: ["demo", "foodie", `${i}`, `demo_foodie_${i}`]
  });
}

// Avatar overrides: cập nhật riêng các profile cần thay ảnh theo yêu cầu.
// Đặt sau vòng tạo user tự động để demo_user_11 và demo_user_13 cũng được ghi đè đúng.
const avatarOverrides = {
  demo_user_01: "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop&q=60", // Minh Tú
  demo_user_05: "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150&auto=format&fit=crop&q=60", // Sơn Chen
  demo_user_11: "https://images.unsplash.com/photo-1522556189639-b150ed9c4330?w=150&auto=format&fit=crop&q=60", // Demo Foodie 11
  demo_user_13: "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=150&auto=format&fit=crop&q=60", // Demo Foodie 13
  demo_user_15: "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=60", // Empty Profile User
  demo_user_16: "https://images.unsplash.com/photo-1600454309261-3dc9b7597637?w=150&auto=format&fit=crop&q=60"  // Bánh Mì Hà Nội
};

users.forEach(user => {
  if (avatarOverrides[user.uid]) {
    user.avatarUrl = avatarOverrides[user.uid];
  }
});

// 2. DATASET: 40 POSTS (MỘT SỐ BÀI VIẾT BIÊN LỖI/MOCK KỊCH BẢN)
const posts = [
  {
    id: "demo_post_001",
    authorUid: "demo_user_01",
    authorName: "Minh Tú",
    authorAvatar: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150&auto=format&fit=crop&q=60",
    caption: "Bún bò tối nay quá đỉnh, nước dùng ngọt lịm từ xương, đầy ắp giò heo và bò viên. Cực kỳ tiến cử nhé anh em!",
    imageUrls: ["https://images.unsplash.com/photo-1597345637412-9fd611e758f3?w=500&auto=format&fit=crop&q=60"],
    videoUrls: [],
    location: "Bún Bò Huế O Xuân",
    linkedGroupId: "demo_group_01",
    linkedBillId: "demo_bill_001", // Bài đăng có liên kết hóa đơn nhóm chia tiền
    likesCount: 4,
    likedBy: ["demo_user_02", "demo_user_03", "demo_user_05", "demo_user_06"],
    commentsCount: 3,
    sharesCount: 1,
    visibility: "public",
    tags: ["bunbo", "vietnamese", "dinner"],
    createdAt: now - 1000 * 60 * 15,
    updatedAt: now - 1000 * 60 * 15
  },
  {
    id: "demo_post_002",
    authorUid: "demo_user_02",
    authorName: "An Rivera",
    authorAvatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=60",
    caption: "Bữa tối Italian lãng mạn. Đế pizza mỏng giòn rụm kết hợp với sốt phô mai béo ngậy ăn không hề ngán tí nào.",
    imageUrls: ["https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&auto=format&fit=crop&q=60"],
    videoUrls: [],
    location: "Pizza 4P's Tràng Tiền",
    linkedGroupId: "",
    linkedBillId: "",
    likesCount: 2,
    likedBy: ["demo_user_01", "demo_user_03"],
    commentsCount: 2,
    sharesCount: 0,
    visibility: "followers_only",
    tags: ["pizza", "italian", "dating"],
    createdAt: now - 1000 * 60 * 60 * 2,
    updatedAt: now - 1000 * 60 * 60 * 2
  },
  {
    id: "demo_post_003",
    authorUid: "demo_user_03",
    authorName: "Khánh Linh",
    authorAvatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=60",
    // Caption cực dài để test nút thu gọn/overflow maxLines = 3 trên Feed
    caption: "Hôm nay khám phá được quán cà phê xinh xắn này cực kỳ hợp ý luôn! Không gian siêu yên tĩnh, ngập tràn ánh nắng tự nhiên qua ô kính lớn, decor gỗ mộc mạc mang cảm giác ấm áp dễ chịu cực kỳ. Thức uống ở đây giá khá hợp lý từ 45k - 65k, recommend mọi người thử món cà phê muối béo ngậy bọt sữa đậm đà hương vị cực kỳ nhé. Rất phù hợp để ngồi học bài, làm việc hay trò chuyện nhẹ nhàng cùng hội bạn thân vào dịp cuối tuần rảnh rỗi nhé cả nhà ơi! Trải nghiệm 9/10 luôn ạ.",
    imageUrls: ["https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=500&auto=format&fit=crop&q=60"],
    videoUrls: [],
    location: "The Note Coffee",
    linkedGroupId: "",
    linkedBillId: "",
    likesCount: 1,
    likedBy: ["demo_user_01"],
    commentsCount: 1,
    sharesCount: 0,
    visibility: "public",
    tags: ["cafe", "coffee", "cozy"],
    createdAt: now - 1000 * 60 * 60 * 5,
    updatedAt: now - 1000 * 60 * 60 * 5
  },
  {
    id: "demo_post_004",
    authorUid: "demo_user_14", // User không có avatar
    authorName: "Trần Vân Anh (Avatar rỗng)",
    authorAvatar: "",
    caption: "Quán bún chả gia truyền gia vị ướp thịt nướng cực kỳ thơm lừng ngào ngạt cả con phố. Nem cua bể cũng siêu giòn.",
    imageUrls: ["https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60"],
    videoUrls: [],
    location: "Bún Chả Hương Liên",
    linkedGroupId: "",
    linkedBillId: "",
    likesCount: 3,
    likedBy: ["demo_user_01", "demo_user_03", "demo_user_05"],
    commentsCount: 0,
    sharesCount: 1,
    visibility: "followers_only",
    tags: ["buncha", "vietnamese"],
    createdAt: now - 1000 * 60 * 60 * 10,
    updatedAt: now - 1000 * 60 * 60 * 10
  },
  // BÀI ĐĂNG KHÔNG CÓ ẢNH (imageUrls rỗng) để test placeholder/layout text-only
  {
    id: "demo_post_005",
    authorUid: "demo_user_05",
    authorName: "Sơn Chen",
    authorAvatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&auto=format&fit=crop&q=60",
    caption: "Mọi người có biết ở quanh khu Hoàn Kiếm có quán salad nào ngon lành mà tính lượng calo cụ thể trong phần ăn không ạ? Xin recommend gấp nhé, đang ăn kiêng nghiêm ngặt quá.",
    imageUrls: [], // KHÔNG CÓ ẢNH
    videoUrls: [],
    location: null,
    linkedGroupId: "",
    linkedBillId: "",
    likesCount: 0,
    likedBy: [],
    commentsCount: 4,
    sharesCount: 0,
    visibility: "public",
    tags: ["diet", "healthy", "salad"],
    createdAt: now - 1000 * 60 * 60 * 18,
    updatedAt: now - 1000 * 60 * 60 * 18
  }
];

const workingFoodImages = [
  "1565299624946-b28f40a0ae38", // Pizza
  "1482049016688-2d3e1b311543", // Egg toast salad
  "1467003909585-2f8a72700288", // Plated salmon
  "1504674900247-0877df9cc836", // Steak / Meat
  "1546069901-ba9599a7e63c", // Salad bowl
  "1567620905732-2d1ec7ab7445", // Pancakes
  "1565958011703-44f9829ba187", // Strawberry cake dessert
  "1484723091739-30a097e8f929", // French toast
  "1473093295043-cdd812d0e601", // Pasta
  "1513104890138-7c749659a591", // Pizza flatlay
  "1476224203421-9ac39bcb3327", // Meat/veggies platter
  "1555939594-58d7cb561ad1", // Meat skewers barbecue
  "1569718212165-3a8278d5f624", // Ramen noodles
  "1540189549336-e6e99c3679fe", // Salad dish
  "1498837167922-ddd27525d352", // Healthy food flatlay
  "1512621776951-a57141f2eefd", // Green salad bowl
  "1493770308161-fd81a649fbb6", // Breakfast table
  "1490645935967-10de6ba17061", // Diet plan meals
  "1506084868230-bb9d95c24759", // Waffles
  "1481931098730-318b6f776db0", // Plated dessert
  "1551183053-bf91a1d81141", // Chocolate cake slice
  "1541167760496-1628856ab772", // Espresso coffee latte
  "1495474472287-4d71bcdd2085", // Table coffee and pastries
  "1517248135467-4c7edcad34c4", // Cozy cafe interior
  "1507133750040-4a8f57021571"  // Milk tea/bubble tea
];

// Tạo thêm posts từ 006 đến 040 tự động cho đa dạng dòng thời gian
for (let i = 6; i <= 40; i++) {
  const id = `demo_post_${String(i).padStart(3, "0")}`;
  const authorIndex = ((i - 1) % users.length);
  const author = users[authorIndex];

  const hasImage = i % 7 !== 0; // Một vài bài đăng không ảnh
  const placeIndex = i % 5;
  const placesCurated = [
    { name: "Phở Thìn Lò Đúc", tag: "pho" },
    { name: "Sushi House", tag: "sushi" },
    { name: "Urban Greens", tag: "healthy" },
    { name: "Bánh Mì Phố Cổ", tag: "banhmi" },
    { name: "Pizza 4P's Tràng Tiền", tag: "pizza" }
  ];
  const place = placesCurated[placeIndex];

  posts.push({
    id,
    authorUid: author.uid,
    authorName: author.displayName,
    authorAvatar: author.avatarUrl,
    caption: `Bài đăng mẫu số ${i} của ${author.displayName}. Trải nghiệm ẩm thực tuyệt vời của tuần này cùng hội bạn thân DineSplit!`,
    imageUrls: hasImage ? [`https://images.unsplash.com/photo-${workingFoodImages[i % workingFoodImages.length]}?w=500&auto=format&fit=crop&q=60`] : [],
    videoUrls: [],
    location: i % 4 === 0 ? null : place.name,
    linkedGroupId: i % 9 === 0 ? "demo_group_02" : "",
    linkedBillId: i % 9 === 0 ? "demo_bill_002" : "",
    likesCount: i % 6,
    likedBy: users.slice(0, i % 6).map(u => u.uid),
    commentsCount: i % 4,
    sharesCount: i % 3,
    visibility: i % 5 === 0 ? "followers_only" : "public",
    tags: ["dining", "dinesplit", place.tag],
    createdAt: now - 1000 * 60 * 60 * i,
    updatedAt: now - 1000 * 60 * 60 * i
  });
}

// 3. DATASET: 80 COMMENTS SUB-COLLECTIONS
const comments = [
  {
    id: "demo_comment_001",
    postId: "demo_post_001",
    authorUid: "demo_user_02",
    authorName: "An Rivera",
    authorAvatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=60",
    content: "Nước dùng có vẻ đậm đà lắm á Tú ơi, cuối tuần này set kèo đi ăn lại nha!",
    createdAt: now - 1000 * 60 * 10
  },
  {
    id: "demo_comment_002",
    postId: "demo_post_001",
    authorUid: "demo_user_03",
    authorName: "Khánh Linh",
    authorAvatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=60",
    content: "Quán này tủ của mình luôn, mọc bò viên dai ngon cực kỳ.",
    createdAt: now - 1000 * 60 * 8
  },
  {
    id: "demo_comment_003",
    postId: "demo_post_001",
    authorUid: "demo_user_05",
    authorName: "Sơn Chen",
    authorAvatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&auto=format&fit=crop&q=60",
    content: "Được, duyệt! Bữa lẩu này tính bill chia tiền cực kỳ mượt luôn.",
    createdAt: now - 1000 * 60 * 5
  },
  {
    id: "demo_comment_004",
    postId: "demo_post_002",
    authorUid: "demo_user_01",
    authorName: "Minh Tú",
    authorAvatar: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150&auto=format&fit=crop&q=60",
    content: "Pizza 4P's Tràng Tiền thì hết ý rồi. An gọi vị gì thế?",
    createdAt: now - 1000 * 60 * 30
  },
  {
    id: "demo_comment_005",
    postId: "demo_post_002",
    authorUid: "demo_user_03",
    authorName: "Khánh Linh",
    authorAvatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=60",
    content: "Món mỳ cua béo ngậy ở đây cũng siêu ngon nha quý dị.",
    createdAt: now - 1000 * 60 * 20
  }
];

// Tạo tự động comments rải rác trên các post để test realtime
for (let i = 6; i <= 80; i++) {
  const postIndex = ((i - 1) % posts.length);
  const postId = posts[postIndex].id;
  const authorIndex = ((i - 1) % users.length);
  const author = users[authorIndex];

  comments.push({
    id: `demo_comment_${String(i).padStart(3, "0")}`,
    postId,
    authorUid: author.uid,
    authorName: author.displayName,
    authorAvatar: author.avatarUrl,
    content: `Bình luận demo số ${i} cực kỳ chân thực để test luồng cập nhật realtime của comment count!`,
    createdAt: now - 1000 * 60 * i
  });
}

// Đồng bộ avatar tác giả trong feed/comment sau khi đã override avatar người dùng.
// Nhờ vậy profile, post card và comment item không bị lệch ảnh khi test UI realtime.
const avatarByUid = Object.fromEntries(users.map(user => [user.uid, user.avatarUrl]));

posts.forEach(post => {
  if (avatarByUid[post.authorUid] !== undefined) {
    post.authorAvatar = avatarByUid[post.authorUid];
  }
});

comments.forEach(comment => {
  if (avatarByUid[comment.authorUid] !== undefined) {
    comment.authorAvatar = avatarByUid[comment.authorUid];
  }
});

// 4. DATASET: 8 GROUPS & 12 BILLS WITH SPECIFIC MEMBERSHIPS
const groups = [];
const groupMembersData = {}; // groupId -> List of members objects

for (let i = 1; i <= 8; i++) {
  const groupId = `demo_group_${String(i).padStart(2, "0")}`;
  // 5 thành viên cụ thể cho nhóm đầu tiên để dễ test, các nhóm khác phân bố linh hoạt
  const memberUids = i === 1
    ? ["demo_user_01", "demo_user_02", "demo_user_03", "demo_user_04", "demo_user_05"]
    : ["demo_user_01", `demo_user_${String(((i - 1) % 15) + 2).padStart(2, "0")}`, "demo_user_02"];

  groups.push({
    id: groupId,
    name: i === 1 ? "Hội Ăn Trưa Đồng Nghiệp" : `Hội Cà Phê Cuối Tuần ${i}`,
    imageUrl: i === 1 ? "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=150&auto=format&fit=crop&q=60" : "",
    memberCount: memberUids.length,
    memberIds: memberUids,
    leftMemberIds: [],
    totalExpense: 0.0, // Sẽ được cập nhật động dựa trên các hóa đơn
    yourBalance: i === 1 ? 75000.0 : -25000.0, // Số dư demo
    createdAt: now - 1000 * 60 * 60 * 24 * i,
    ownerId: "demo_user_01"
  });

  // Lưu thông tin member chi tiết để nạp vào sub-collection
  groupMembersData[groupId] = memberUids.map(uid => {
    const user = users.find(u => u.uid === uid) || users[0];
    return {
      id: uid,
      name: user.displayName,
      initial: user.displayName.trim().firstOrNull?.() || "U",
      isMe: uid === "demo_user_01"
    };
  });
}

// 5. DATASET: 12 SPLIT BILLS
const bills = [];
for (let i = 1; i <= 12; i++) {
  const billId = `demo_bill_${String(i).padStart(3, "0")}`;
  const groupId = `demo_group_${String(((i - 1) % 8) + 1).padStart(2, "0")}`;
  const amount = 150000.0 + i * 45000.0;

  // Thành viên tham gia
  const members = groupMembersData[groupId] || [];
  const memberIds = members.map(m => m.id);

  // Shares division
  const shares = {};
  const shareAmount = amount / memberIds.length;
  memberIds.forEach(mid => {
    shares[mid] = parseFloat(shareAmount.toFixed(2));
  });

  const method = i % 3 === 0 ? "CUSTOM" : i % 3 === 1 ? "EQUAL" : "ITEMIZED";
  const paidMemberIds = i % 4 === 0
    ? memberIds // PAID
    : i % 4 === 1
    ? [] // UNPAID
    : [memberIds[1] || "demo_user_02"]; // PARTIAL

  // Mock BillItems
  const items = [
    {
      id: `item_${billId}_1`,
      name: "Món ăn khai vị",
      price: amount * 0.4,
      sharedByMemberIds: memberIds
    },
    {
      id: `item_${billId}_2`,
      name: "Món chính & Nước uống",
      price: amount * 0.6,
      sharedByMemberIds: memberIds.slice(0, 2)
    }
  ];

  bills.push({
    id: billId,
    groupId,
    name: i === 1 ? "Bữa Bún bò O Xuân tụ tập" : `Hóa đơn ẩm thực số ${i}`,
    totalAmount: amount,
    payerId: "demo_user_01", // Tú luôn trả trước để dễ test
    method,
    items,
    shares,
    paidMemberIds,
    date: now - 1000 * 60 * 60 * 10 * i
  });
}

// Cập nhật lại totalExpense cho mỗi group dựa trên tổng tiền hóa đơn tương ứng
groups.forEach(group => {
  const groupBills = bills.filter(b => b.groupId === group.id);
  group.totalExpense = groupBills.reduce((sum, b) => sum + b.totalAmount, 0.0);
});

// 6. DATASET: 30 NOTIFICATIONS
const notifications = [];
for (let i = 1; i <= 30; i++) {
  const notificationId = `demo_notification_${String(i).padStart(3, "0")}`;
  const type = i % 3 === 0 ? "ACTIVITY_UPDATE" : i % 3 === 1 ? "BILL_CREATED" : "PAYMENT_PENDING";

  let title = "Tương tác mạng xã hội";
  let subtitle = "Ai đó đã thích bài viết của bạn";
  let deepLinkDestination = null;
  let deepLinkTargetId = null;

  if (type === "ACTIVITY_UPDATE") {
    const isLike = i % 2 === 0;
    title = isLike ? "An Rivera đã thích bài viết của bạn" : "Khánh Linh đã bình luận về bài viết";
    subtitle = isLike ? "Bún bò tối nay quá đỉnh..." : "Nhìn ngon quá, lần sau cho mình đi cùng nhé!";
    deepLinkDestination = "ACTIVITY_DETAIL";
    deepLinkTargetId = "demo_post_001";
  } else if (type === "BILL_CREATED") {
    title = "Hóa đơn nhóm mới được tạo";
    subtitle = "Minh Tú đã thêm hóa đơn: Bữa Bún bò O Xuân tụ tập";
    deepLinkDestination = "BILL_DETAIL";
    deepLinkTargetId = "demo_bill_001";
  } else {
    title = "Bạn có hóa đơn cần thanh toán";
    subtitle = "Hội Ăn Trưa Đồng Nghiệp đang chờ bạn settle up số tiền 65.000đ";
    deepLinkDestination = "BILL_DETAIL";
    deepLinkTargetId = "demo_bill_001";
  }

  notifications.push({
    id: notificationId,
    userId: "demo_user_01", // Nạp cho Tú để Tú mở app lên là thấy ngay hòm thư đỏ
    title,
    subtitle,
    type,
    relatedId: deepLinkTargetId,
    isRead: i % 5 === 0,
    createdAt: now - 1000 * 60 * 15 * i,
    updatedAt: now - 1000 * 60 * 15 * i,
    deepLinkDestination,
    deepLinkTargetId
  });
}

// Personal Finance Mock Data for Minh Tú (demo_user_01)
const personalCategories = [
  { id: "c_food", name: "Dining Out", icon: "FD", type: "EXPENSE", isCustom: false, description: "Restaurants, cafes, and delivery.", amountLabel: "0 VND", progress: 0, isActive: true },
  { id: "c_grocery", name: "Groceries", icon: "GR", type: "EXPENSE", isCustom: false, description: "Supermarkets and local markets.", amountLabel: "0 VND", progress: 0, isActive: false },
  { id: "c_transit", name: "Transit", icon: "TR", type: "EXPENSE", isCustom: false, description: "Rideshares and public transport.", amountLabel: "0 VND", progress: 0, isActive: false },
  { id: "c_fun", name: "Entertainment", icon: "EN", type: "EXPENSE", isCustom: true, description: "Movies, events, and subscriptions.", amountLabel: "0 VND", progress: 0, isActive: false },
  { id: "c_salary", name: "Salary", icon: "SL", type: "INCOME", isCustom: false, description: "Monthly fixed salary income.", amountLabel: "0 VND", progress: 0, isActive: true },
  { id: "c_bonus", name: "Bonus", icon: "BN", type: "INCOME", isCustom: false, description: "Project and performance rewards.", amountLabel: "0 VND", progress: 0, isActive: false },
  { id: "c_gift", name: "Gift", icon: "GF", type: "INCOME", isCustom: true, description: "Personal gifts and contributions.", amountLabel: "0 VND", progress: 0, isActive: false },
  { id: "c_other_income", name: "Other", icon: "OT", type: "INCOME", isCustom: true, description: "Other incoming cash flows.", amountLabel: "0 VND", progress: 0, isActive: false }
];

const personalWallets = [
  { id: "wallet_cash", userId: "demo_user_01", name: "Tiền mặt", walletType: "CASH", balance: 2500000.0, color: "#4CAF50", isArchived: false, createdAt: now, updatedAt: now },
  { id: "wallet_techcom", userId: "demo_user_01", name: "Techcombank", walletType: "BANK", balance: 15450000.0, color: "#E53935", isArchived: false, createdAt: now, updatedAt: now },
  { id: "wallet_momo", userId: "demo_user_01", name: "Ví Momo", walletType: "EWALLET", balance: 850000.0, color: "#D81B60", isArchived: false, createdAt: now, updatedAt: now }
];

const personalTransactions = [
  {
    id: "txn_001",
    userId: "demo_user_01",
    amount: 18000000.0,
    type: "INCOME",
    categoryId: "c_salary",
    category: "Salary",
    note: "Lương tháng 5",
    date: now - 1000 * 60 * 60 * 24 * 5,
    createdAt: now - 1000 * 60 * 60 * 24 * 5,
    updatedAt: now - 1000 * 60 * 60 * 24 * 5,
    source: "MANUAL",
    sourceGroupId: "",
    sourceBillId: "",
    recurringRuleId: "",
    receiptImageUrl: "",
    walletId: "wallet_techcom"
  },
  {
    id: "txn_002",
    userId: "demo_user_01",
    amount: 120000.0,
    type: "EXPENSE",
    categoryId: "c_food",
    category: "Dining Out",
    note: "Ăn tối bún chả Hương Liên",
    date: now - 1000 * 60 * 60 * 24 * 2,
    createdAt: now - 1000 * 60 * 60 * 24 * 2,
    updatedAt: now - 1000 * 60 * 60 * 24 * 2,
    source: "MANUAL",
    sourceGroupId: "",
    sourceBillId: "",
    recurringRuleId: "",
    receiptImageUrl: "",
    walletId: "wallet_cash"
  },
  {
    id: "txn_003",
    userId: "demo_user_01",
    amount: 450000.0,
    type: "EXPENSE",
    categoryId: "c_grocery",
    category: "Groceries",
    note: "Mua sắm thực phẩm Coopmart",
    date: now - 1000 * 60 * 60 * 24 * 1,
    createdAt: now - 1000 * 60 * 60 * 24 * 1,
    updatedAt: now - 1000 * 60 * 60 * 24 * 1,
    source: "MANUAL",
    sourceGroupId: "",
    sourceBillId: "",
    recurringRuleId: "",
    receiptImageUrl: "",
    walletId: "wallet_momo"
  },
  {
    id: "txn_004",
    userId: "demo_user_01",
    amount: 45000.0,
    type: "EXPENSE",
    categoryId: "c_transit",
    category: "Transit",
    note: "GrabBike đi làm sáng",
    date: now - 1000 * 60 * 60 * 6,
    createdAt: now - 1000 * 60 * 60 * 6,
    updatedAt: now - 1000 * 60 * 60 * 6,
    source: "MANUAL",
    sourceGroupId: "",
    sourceBillId: "",
    recurringRuleId: "",
    receiptImageUrl: "",
    walletId: "wallet_momo"
  },
  {
    id: "txn_005",
    userId: "demo_user_01",
    amount: 180000.0,
    type: "EXPENSE",
    categoryId: "c_fun",
    category: "Entertainment",
    note: "Vé xem phim CGV",
    date: now - 1000 * 60 * 60 * 12,
    createdAt: now - 1000 * 60 * 60 * 12,
    updatedAt: now - 1000 * 60 * 60 * 12,
    source: "MANUAL",
    sourceGroupId: "",
    sourceBillId: "",
    recurringRuleId: "",
    receiptImageUrl: "",
    walletId: "wallet_cash"
  }
];

const personalGoals = [
  {
    id: "goal_001",
    userId: "demo_user_01",
    title: "Quỹ mua Macbook M4",
    targetAmount: 35000000.0,
    currentAmount: 12000000.0,
    categoryId: "c_fun",
    deadlineAt: now + 1000 * 60 * 60 * 24 * 90,
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now
  },
  {
    id: "goal_002",
    userId: "demo_user_01",
    title: "Đi du lịch Phú Quốc",
    targetAmount: 8000000.0,
    currentAmount: 3000000.0,
    categoryId: "c_transit",
    deadlineAt: now + 1000 * 60 * 60 * 24 * 45,
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now
  }
];

const personalReminders = [
  {
    id: "reminder_001",
    userId: "demo_user_01",
    categoryId: "", // Overall
    categoryName: "Overall",
    budgetAmount: 10000000.0,
    currentSpent: 795000.0,
    threshold: 0.8,
    reminderType: "MONTHLY",
    isEnabled: true,
    lastAlertedAt: 0,
    createdAt: now,
    updatedAt: now
  },
  {
    id: "reminder_002",
    userId: "demo_user_01",
    categoryId: "c_food",
    categoryName: "Dining Out",
    budgetAmount: 3000000.0,
    currentSpent: 120000.0,
    threshold: 0.8,
    reminderType: "MONTHLY",
    isEnabled: true,
    lastAlertedAt: 0,
    createdAt: now,
    updatedAt: now
  }
];

const personalRecurringRules = [
  {
    id: "rule_001",
    userId: "demo_user_01",
    name: "Gói Netflix Family",
    amount: 260000.0,
    type: "EXPENSE",
    categoryId: "c_fun",
    categoryName: "Entertainment",
    cadence: "MONTHLY",
    dayOfMonth: 15,
    nextRunAt: now + 1000 * 60 * 60 * 24 * 10,
    isEnabled: true,
    createdAt: now,
    updatedAt: now
  },
  {
    id: "rule_002",
    userId: "demo_user_01",
    name: "Spotify Premium",
    amount: 59000.0,
    type: "EXPENSE",
    categoryId: "c_fun",
    categoryName: "Entertainment",
    cadence: "MONTHLY",
    dayOfMonth: 5,
    nextRunAt: now + 1000 * 60 * 60 * 24 * 1,
    isEnabled: true,
    createdAt: now,
    updatedAt: now
  }
];

// 7. HELPER: TRÌNH TỰ ĐĂNG KÝ AUTHENTICATION TỰ ĐỘNG
async function createAuthUsers() {
  const authUsers = [
    {
      uid: "demo_user_01",
      email: "minhtu.demo@example.com",
      password: "123456",
      displayName: "Minh Tú"
    },
    {
      uid: "demo_user_02",
      email: "arivera.demo@example.com",
      password: "123456",
      displayName: "An Rivera"
    },
    {
      uid: "demo_user_03",
      email: "khanhlinh.demo@example.com",
      password: "123456",
      displayName: "Khánh Linh"
    }
  ];

  for (const user of authUsers) {
    try {
      await auth.createUser({
        uid: user.uid,
        email: user.email,
        password: user.password,
        displayName: user.displayName
      });
      console.log(`Đã tạo tài khoản Auth: ${user.email} (password: 123456)`);
    } catch (error) {
      if (error.code === "auth/uid-already-exists" || error.code === "auth/email-already-exists") {
        console.log(`Tài khoản Auth đã tồn tại sẵn: ${user.email}`);
      } else {
        throw error;
      }
    }
  }
}

// 8. HELPER: DỌN SẠCH DỮ LIỆU CŨ TRƯỚC KHI SEED FRESH
async function deleteCollectionRecursively(collectionName) {
  const snapshot = await db.collection(collectionName).get();
  for (const doc of snapshot.docs) {
    await db.recursiveDelete(doc.ref);
  }
  console.log(`Đã dọn sạch đệ quy collection: ${collectionName}`);
}

async function resetDatabase() {
  await deleteCollectionRecursively("users");
  await deleteCollectionRecursively("usernames");
  await deleteCollectionRecursively("posts");
  await deleteCollectionRecursively("groups");
  await deleteCollectionRecursively("user_notifications");
  await deleteCollectionRecursively("user_personal");
  await deleteCollectionRecursively("qr_payments");
  await deleteCollectionRecursively("app_metadata");
  console.log("Đã dọn sạch cơ sở dữ liệu Firestore thành công!");
}

// 9. BATCH COMMIT: GHI DỮ LIỆU THEO CHUNKS ĐỂ TRÁNH QUÁ TẢI FIRESTORE
async function commitInChunks(writeOperations, chunkSize = 300) {
  for (let i = 0; i < writeOperations.length; i += chunkSize) {
    const batch = db.batch();
    const chunk = writeOperations.slice(i, i + chunkSize);

    for (const op of chunk) {
      batch.set(op.ref, op.data, { merge: true });
    }

    await batch.commit();
    console.log(`Đã ghi thành công chunk gồm ${chunk.length} tài liệu...`);
  }
}

// 10. TRÌNH TỰ SEED DỮ LIỆU CHÍNH
function convertDateFields(obj) {
  if (!obj || typeof obj !== "object") return obj;
  if (Array.isArray(obj)) {
    return obj.map(convertDateFields);
  }
  const result = {};
  for (const [key, value] of Object.entries(obj)) {
    if ((key === "createdAt" || key === "updatedAt" || key === "date" || key === "seededAt") && typeof value === "number") {
      result[key] = new Date(value);
    } else if (typeof value === "object") {
      result[key] = convertDateFields(value);
    } else {
      result[key] = value;
    }
  }
  return result;
}

async function seedDatabase() {
  const metadataRef = db.collection("app_metadata").doc("demo_seed");
  const metadataSnap = await metadataRef.get();

  if (metadataSnap.exists && metadataSnap.data().version === SEED_VERSION) {
    console.log(`Phiên bản Seeder ${SEED_VERSION} đã được nạp trước đó rồi. Bỏ qua.`);
    return;
  }

  // Khởi tạo Auth users trước
  await createAuthUsers();

  const operations = [];

  // Seed Users
  users.forEach(u => {
    u.usernameLower = u.username.toLowerCase();
    operations.push({
      ref: db.collection("users").doc(u.uid),
      data: convertDateFields(u)
    });

    // Seed usernames registry
    operations.push({
      ref: db.collection("usernames").doc(u.username.toLowerCase()),
      data: {
        uid: u.uid,
        usernameLower: u.username.toLowerCase(),
        username: u.username,
        claimedAt: now,
        updatedAt: now
      }
    });
  });

  // Seed Posts
  posts.forEach(p => {
    operations.push({
      ref: db.collection("posts").doc(p.id),
      data: convertDateFields(p)
    });
  });

  // Seed Comments (Lưu vào sub-collection posts/{postId}/comments)
  comments.forEach(c => {
    operations.push({
      ref: db.collection("posts").doc(c.postId).collection("comments").doc(c.id),
      data: convertDateFields(c)
    });
  });

  // Seed Groups & Members
  groups.forEach(g => {
    operations.push({
      ref: db.collection("groups").doc(g.id),
      data: convertDateFields(g)
    });

    const members = groupMembersData[g.id] || [];
    members.forEach(m => {
      operations.push({
        ref: db.collection("groups").doc(g.id).collection("members").doc(m.id),
        data: convertDateFields(m)
      });
    });
  });

  // Seed Bills
  bills.forEach(b => {
    operations.push({
      ref: db.collection("groups").doc(b.groupId).collection("bills").doc(b.id),
      data: convertDateFields(b)
    });
  });

  // Seed Notifications
  notifications.forEach(n => {
    operations.push({
      ref: db.collection("user_notifications").doc(n.userId).collection("notifications").doc(n.id),
      data: convertDateFields(n)
    });
  });

  // Seed Personal Finance subcollections
  // 1. Categories for all users
  users.forEach(u => {
    personalCategories.forEach(cat => {
      operations.push({
        ref: db.collection("user_personal").doc(u.uid).collection("categories").doc(cat.id),
        data: convertDateFields(cat)
      });
    });
  });

  // 2. Wallets, Transactions, Goals, Reminders, Recurring rules for demo_user_01
  personalWallets.forEach(w => {
    operations.push({
      ref: db.collection("user_personal").doc("demo_user_01").collection("wallets").doc(w.id),
      data: convertDateFields(w)
    });
  });

  personalTransactions.forEach(t => {
    operations.push({
      ref: db.collection("user_personal").doc("demo_user_01").collection("transactions").doc(t.id),
      data: convertDateFields(t)
    });
  });

  personalGoals.forEach(g => {
    operations.push({
      ref: db.collection("user_personal").doc("demo_user_01").collection("goals").doc(g.id),
      data: convertDateFields(g)
    });
  });

  personalReminders.forEach(r => {
    operations.push({
      ref: db.collection("user_personal").doc("demo_user_01").collection("reminders").doc(r.id),
      data: convertDateFields(r)
    });
  });

  personalRecurringRules.forEach(rr => {
    operations.push({
      ref: db.collection("user_personal").doc("demo_user_01").collection("recurring_rules").doc(rr.id),
      data: convertDateFields(rr)
    });
  });

  // Seed Metadata
  operations.push({
    ref: metadataRef,
    data: convertDateFields({
      seeded: true,
      version: SEED_VERSION,
      seededAt: Date.now(),
      summary: {
        users: users.length,
        posts: posts.length,
        comments: comments.length,
        groups: groups.length,
        bills: bills.length,
        notifications: notifications.length
      }
    })
  });

  console.log("Đang ghi hàng loạt dữ liệu mẫu lên Firestore...");
  await commitInChunks(operations);
  console.log("Nạp dữ liệu mẫu (Seeder) thành công mỹ mãn!");
}

async function main() {
  const args = process.argv.slice(2);
  const shouldReset = args.includes("--reset");
  const shouldSeed = !args.includes("--reset") || args.includes("--seed");

  if (shouldReset) {
    console.log("Bắt đầu làm sạch cơ sở dữ liệu...");
    await resetDatabase();
  }

  if (shouldSeed) {
    console.log("Bắt đầu nạp dữ liệu mẫu...");
    await seedDatabase();
  }

  console.log("Đã hoàn tất toàn bộ công việc!");
  process.exit(0);
}

main().catch(err => {
  console.error("Lỗi trong quá trình Seeder:", err);
  process.exit(1);
});
