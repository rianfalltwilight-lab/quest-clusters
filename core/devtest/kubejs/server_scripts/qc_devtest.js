// Quest Clusters dev self-test: records what FTB Quests' KubeJS events and binding did, for the test to check.
PlayerEvents.loggedIn(event => {
  const p = event.player
  p.persistentData.putString('qc_kjs_binding', String(FTBQuests.getObject(p.level, '00000000000000D5')))
  p.persistentData.putInt('qc_kjs_count', 0)
  p.persistentData.putBoolean('qc_kjs_tag', false)
})

FTBQuestsEvents.completed(event => {
  const p = event.player
  if (!p) return
  const d = p.persistentData
  d.putString('qc_kjs_last', event.object.toString())
  if (event.object.toString() == '00000000000000D7') {
    d.putInt('qc_kjs_count', d.getInt('qc_kjs_count') + 1)
    d.putBoolean('qc_kjs_data', p.data.ftbquests.isCompleted('00000000000000D7'))
  }
})

FTBQuestsEvents.completed('#qc_tag', event => {
  if (event.player) event.player.persistentData.putBoolean('qc_kjs_tag', true)
})
