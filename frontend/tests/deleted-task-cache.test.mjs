import assert from 'node:assert/strict'
import { beforeEach, test } from 'node:test'
import { clearDeletedTaskCaches, readDeletedTasks, writeDeletedTasks } from '../src/utils/deleted-task-cache.ts'

beforeEach(() => {
  globalThis.localStorage = Object.create({
    getItem(key) { return this[key] ?? null },
    setItem(key, value) { this[key] = String(value) },
    removeItem(key) { delete this[key] },
  })
})

test('isolates personal history by user and team history by user/team', () => {
  writeDeletedTasks(1, 'personal', [{ id: 10 }])
  writeDeletedTasks(1, 'team:3', [{ id: 11 }])
  assert.deepEqual(readDeletedTasks(2, 'personal'), [])
  assert.deepEqual(readDeletedTasks(2, 'team:3'), [])
  assert.deepEqual(readDeletedTasks(1, 'team:4'), [])
  assert.deepEqual(readDeletedTasks(1, 'personal'), [{ id: 10 }])
  assert.deepEqual(readDeletedTasks(1, 'team:3'), [{ id: 11 }])
})

test('anonymous sessions cannot read or write history', () => {
  writeDeletedTasks(null, 'personal', [{ id: 10 }])
  assert.deepEqual(readDeletedTasks(null, 'personal'), [])
  assert.deepEqual(Object.keys(localStorage), [])
})

test('discards legacy unowned data without migrating it to the next user', () => {
  localStorage.setItem('deletedTasks', '[{"id":1}]')
  localStorage.setItem('deletedTeamTasks_3', '[{"id":2}]')
  assert.deepEqual(readDeletedTasks(2, 'personal'), [])
  assert.equal(localStorage.getItem('deletedTasks'), null)
  assert.equal(localStorage.getItem('deletedTeamTasks_3'), null)
})

test('malformed and non-array cache contents do not break the page', () => {
  for (const value of ['broken', '{}', 'null']) {
    localStorage.setItem('task-history:v1:1:personal', value)
    assert.deepEqual(readDeletedTasks(1, 'personal'), [])
  }
})

test('logout cleanup removes histories and preserves unrelated settings', () => {
  localStorage.setItem('theme', 'dark')
  writeDeletedTasks(1, 'personal', [{ id: 1 }])
  writeDeletedTasks(2, 'team:3', [{ id: 2 }])
  clearDeletedTaskCaches()
  assert.deepEqual(readDeletedTasks(1, 'personal'), [])
  assert.deepEqual(readDeletedTasks(2, 'team:3'), [])
  assert.equal(localStorage.getItem('theme'), 'dark')
})

test('storage failures do not turn a successful server deletion into an error', () => {
  globalThis.localStorage = {
    getItem() { throw new Error('unavailable') },
    setItem() { throw new Error('quota') },
    removeItem() { throw new Error('unavailable') },
  }
  assert.deepEqual(readDeletedTasks(1, 'personal'), [])
  assert.doesNotThrow(() => writeDeletedTasks(1, 'personal', [{ id: 1 }]))
  assert.doesNotThrow(clearDeletedTaskCaches)
})
