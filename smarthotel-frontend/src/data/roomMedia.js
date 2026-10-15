// Room photos supplied for this project. The tall information screenshots are
// intentionally excluded from the galleries; their verified facts are summarized below.
const roomMedia = {
  RT01: {
    count: 3,
    description: '42㎡豪华大床房，配备1张1.83米特大床。图片介绍显示房间位于15–38层，提供落地窗、免费 Wi-Fi、浴缸和智能马桶。',
    highlights: ['1.83米特大床', '15–38层', '落地窗', '浴缸']
  },
  RT02: {
    count: 3,
    description: '42㎡豪华双床房，配备2张1.38米双人床。图片介绍显示房间位于15–38层，提供落地窗、免费 Wi-Fi、浴缸和智能马桶。',
    highlights: ['2张1.38米床', '15–38层', '落地窗', '浴缸']
  },
  RT03: {
    count: 4,
    description: '42㎡行政大床房，配备1张1.83米特大床。图片介绍显示房间位于39–42层，提供落地窗、免费 Wi-Fi、浴缸及行政酒廊礼遇。',
    highlights: ['1.83米特大床', '39–42层', '行政酒廊礼遇', '浴缸']
  },
  RT04: {
    count: 4,
    description: '42㎡行政双床房，配备2张1.38米双人床。图片介绍显示房间位于39–42层，提供落地窗、免费 Wi-Fi、浴缸及行政酒廊礼遇。',
    highlights: ['2张1.38米床', '39–42层', '行政酒廊礼遇', '浴缸']
  },
  RT05: {
    count: 3,
    description: '57㎡高级大床房，配备1张1.83米特大床。图片介绍显示房间位于15–38层，提供落地窗、免费 Wi-Fi、浴缸和咖啡机。',
    highlights: ['1.83米特大床', '15–38层', '落地窗', '咖啡机']
  },
  RT06: {
    count: 4,
    description: '57㎡行政高级大床房，配备1张1.83米特大床。图片介绍显示房间位于39–42层，提供城景、免费 Wi-Fi、浴缸及行政酒廊礼遇。',
    highlights: ['1.83米特大床', '39–42层', '城景', '行政酒廊礼遇']
  },
  RT07: {
    count: 3,
    description: '42㎡豪华河景大床房，配备大床与河景视野。房间照片展示了休息区和落地窗。',
    highlights: ['大床', '河景', '落地窗']
  },
  RT08: {
    count: 3,
    description: '42㎡豪华河景双床房，配备双床与河景视野。房间照片展示了休息区和浴缸。',
    highlights: ['双床', '河景', '浴缸']
  },
  RT09: {
    count: 3,
    description: '42㎡行政河景大床房，配备大床与河景视野，适合偏好高层景观的住客。',
    highlights: ['大床', '河景', '行政房型']
  },
  RT10: {
    count: 3,
    description: '42㎡行政河景双床房，配备双床与河景视野，适合结伴入住。',
    highlights: ['双床', '河景', '行政房型']
  },
  RT11: {
    count: 5,
    cover: 4,
    description: '42㎡海底小纵队主题豪华大床房。照片展示了主题床品、儿童帐篷及亲子用品。',
    highlights: ['主题床品', '儿童帐篷', '亲子用品']
  },
  RT12: {
    count: 6,
    description: '42㎡ Hape 新丝路亲子主题豪华大床房。照片展示了主题床品、儿童帐篷和亲子布置。',
    highlights: ['亲子主题', '儿童帐篷', '主题床品']
  },
  RT13: {
    count: 6,
    description: '42㎡小黄鸭主题豪华大床房。照片展示了主题床品、玩具和儿童活动区域。',
    highlights: ['小黄鸭主题', '儿童活动区域', '主题床品']
  },
  RT14: {
    count: 5,
    description: '65㎡豪华套房，配备1张1.83米特大床。图片介绍显示房间位于35–42层，提供落地窗、浴缸及行政酒廊礼遇。',
    highlights: ['1.83米特大床', '35–42层', '落地窗', '行政酒廊礼遇']
  },
  RT15: {
    count: 7,
    description: '100㎡行政套房，配备1张1.83米特大床。图片介绍显示房间位于31–38层，提供落地窗、浴缸及行政酒廊礼遇。',
    highlights: ['1.83米特大床', '31–38层', '落地窗', '行政酒廊礼遇']
  }
}

const imagePath = (typeCode, number) =>
  `/images/rooms/${typeCode}/${String(number).padStart(2, '0')}.jpg`

export const getRoomMedia = typeCode => roomMedia[typeCode] || null

export const getRoomCover = typeCode => {
  const media = getRoomMedia(typeCode)
  return media ? imagePath(typeCode, media.cover || 1) : null
}

export const getRoomPhotos = typeCode => {
  const media = getRoomMedia(typeCode)
  return media
    ? Array.from({ length: media.count }, (_, index) => imagePath(typeCode, index + 1))
    : []
}
