// dbml 과 openapi 를 전용 파서로 연다.
// 정규식 기반 검사는 문법이 깨져도 통과해 버리므로 이 검증이 따로 필요하다.
// 사용: node tools/validate_specs.js <프로젝트 루트>
const { Parser } = require('@dbml/core');
const SwaggerParser = require('@apidevtools/swagger-parser');
const fs = require('fs');

const base = process.argv[2];
try {
  const db = Parser.parse(fs.readFileSync(base + '/docs/02_데이터모델링/schema.dbml', 'utf8'), 'dbml');
  console.log('DBML OK tables=' + db.schemas[0].tables.length + ' refs=' + db.schemas[0].refs.length);
} catch (e) {
  const g = (e.diags || [])[0];
  console.log('DBML FAIL ' + (g ? g.message + ' @line ' + g.location.start.line : e.message));
}
SwaggerParser.validate(base + '/docs/03_API명세/openapi.yaml')
  .then(() => console.log('OPENAPI OK'))
  .catch((e) => console.log('OPENAPI FAIL ' + e.message.split('\n')[0]));
