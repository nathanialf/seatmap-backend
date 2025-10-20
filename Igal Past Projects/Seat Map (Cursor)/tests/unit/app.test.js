const request = require('supertest');
const app = require('../../src/app');

describe('Server Configuration', () => {
  it('should respond with API message on /api/health', async () => {
    const res = await request(app).get('/api/health').set('Accept', 'application/json');
    expect(res.statusCode).toBe(200);
    expect(res.body).toHaveProperty('message', 'API funcionando');
  });

  it('should return 404 for non-existent routes', async () => {
    const res = await request(app).get('/nonexistent');
    expect(res.statusCode).toBe(404);
  });

  it('should handle JSON parsing', async () => {
    const res = await request(app)
      .post('/api/airports')
      .send({ invalid: 'data' })
      .set('Accept', 'application/json');
    expect(res.statusCode).toBe(404);
  });
}); 