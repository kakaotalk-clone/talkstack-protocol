// buf generate 로 src/generated 아래에 생성된 타입을 재수출합니다.
// 소비 측은 항상 '@kakaoclone/protocol' 에서만 import 합니다.
export * from './generated/common.js';
export * from './generated/ws_frame.js';

// events.proto / chat_service.proto / room_service.proto 는 서버 전용이라
// 웹 번들에 포함하지 않습니다.
