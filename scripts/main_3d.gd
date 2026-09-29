extends Node3D

# Henry Western - 3D playable foundation
# Offline single-player architecture for Android.
# Systems are kept lightweight and modular so the world can grow by regions.

const WORLD_SIZE := Vector2(160.0, 110.0)
const PLAYER_SPEED := 6.0
const HORSE_SPEED := 10.0
const INTERACTION_DISTANCE := 4.0
const BULLET_DAMAGE := 34
const ENEMY_DAMAGE := 8

var player: CharacterBody3D
var camera: Camera3D
var horse_position := Vector3(12.0, 0.8, -6.0)
var sheriff_position := Vector3(-8.0, 0.0, -4.0)
var mission_target_position := Vector3(8.0, 0.0, -4.0)
var money := 25
var health := 100
var wanted := 0
var mounted := false
var mission_state := "not_started"
var mission_reward := 50
var mission_target_health := 100
var enemies: Array[Node3D] = []
var hud_label: Label
var objective_label: Label
var status_label: Label
var horse_marker: MeshInstance3D

func _ready() -> void:
	player = $Player
	camera = $Camera3D
	_build_placeholder_world()
	_build_hud()
	_update_camera()
	_update_hud()

func _physics_process(_delta: float) -> void:
	if player == null:
		return

	var input_2d := Input.get_vector("move_left", "move_right", "move_up", "move_down")
	var direction := Vector3(input_2d.x, 0.0, input_2d.y)
	var speed := HORSE_SPEED if mounted else PLAYER_SPEED
	player.velocity = direction * speed
	player.move_and_slide()
	player.position.x = clamp(player.position.x, -WORLD_SIZE.x * 0.5, WORLD_SIZE.x * 0.5)
	player.position.z = clamp(player.position.z, -WORLD_SIZE.y * 0.5, WORLD_SIZE.y * 0.5)

	if Input.is_action_just_pressed("interact"):
		_interact()
	if Input.is_action_just_pressed("fire"):
		_fire()

	_update_enemies()
	_update_camera()
	_update_hud()

func _update_camera() -> void:
	camera.position = player.position + Vector3(0.0, 7.5, 11.0)
	camera.look_at(player.position + Vector3(0.0, 0.8, 0.0))

func _build_placeholder_world() -> void:
	# Original primitives preserve the existing large-map layout while gameplay is built.
	_create_box(Vector3(8, 3, 6), Vector3(-28, 1.5, -12), Color(0.38, 0.20, 0.10), "DustyCreekSaloon")
	_create_box(Vector3(6, 4, 6), Vector3(-18, 2, -12), Color(0.48, 0.28, 0.12), "DustyCreekStore")
	_create_box(Vector3(10, 3, 8), Vector3(20, 1.5, 24), Color(0.32, 0.18, 0.09), "RanchoRedRock")
	_create_box(Vector3(7, 5, 7), Vector3(48, 2.5, 34), Color(0.20, 0.20, 0.18), "BlackRidgeMine")
	_create_box(Vector3(5, 2, 5), Vector3(35, 1, -30), Color(0.26, 0.12, 0.08), "BanditCamp")
	_create_marker(sheriff_position, Color(0.85, 0.75, 0.25), "Sheriff")
	horse_marker = _create_marker(horse_position, Color(0.28, 0.16, 0.08), "Horse")
	_create_marker(mission_target_position, Color(0.65, 0.18, 0.12), "MissionTarget")

	# A small group of original enemy placeholders around the bandit region.
	_create_enemy(Vector3(32, 1, -27), "Bandit_A")
	_create_enemy(Vector3(39, 1, -30), "Bandit_B")
	_create_enemy(Vector3(35, 1, -35), "Bandit_C")

func _create_box(size: Vector3, position: Vector3, color: Color, node_name: String) -> void:
	var body := StaticBody3D.new()
	body.name = node_name
	body.position = position
	var mesh_instance := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = size
	mesh_instance.mesh = mesh
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	material.roughness = 0.9
	mesh_instance.material_override = material
	body.add_child(mesh_instance)
	var collision := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = size
	collision.shape = shape
	body.add_child(collision)
	add_child(body)

func _create_marker(position: Vector3, color: Color, node_name: String) -> MeshInstance3D:
	var marker := MeshInstance3D.new()
	var mesh := CylinderMesh.new()
	mesh.top_radius = 0.35
	mesh.bottom_radius = 0.5
	mesh.height = 1.0
	marker.mesh = mesh
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	marker.material_override = material
	marker.position = position + Vector3(0, 0.5, 0)
	marker.name = node_name
	add_child(marker)
	return marker

func _create_enemy(position: Vector3, node_name: String) -> void:
	var enemy := MeshInstance3D.new()
	enemy.name = node_name
	var mesh := CapsuleMesh.new()
	mesh.radius = 0.42
	mesh.height = 1.8
	enemy.mesh = mesh
	var material := StandardMaterial3D.new()
	material.albedo_color = Color(0.35, 0.04, 0.03)
	enemy.material_override = material
	enemy.position = position
	enemy.set_meta("health", 100)
	enemy.set_meta("active", true)
	add_child(enemy)
	enemies.append(enemy)

func _interact() -> void:
	if player.position.distance_to(horse_position) <= INTERACTION_DISTANCE:
		mounted = not mounted
		if mounted:
			status_label.text = "Montado no cavalo"
		else:
			status_label.text = "Você desmontou"
		return

	if player.position.distance_to(sheriff_position) <= INTERACTION_DISTANCE:
		if mission_state == "not_started":
			mission_state = "active"
			status_label.text = "Missão aceita: encontre o procurado"
		elif mission_state == "return":
			mission_state = "completed"
			money += mission_reward
			wanted = max(0, wanted - 1)
			status_label.text = "Missão concluída! +$%d" % mission_reward

func _fire() -> void:
	var nearest: Node3D = null
	var nearest_distance := 9999.0
	for enemy in enemies:
		if not is_instance_valid(enemy) or not enemy.get_meta("active", false):
			continue
		var distance := player.position.distance_to(enemy.position)
		if distance < nearest_distance and distance <= 22.0:
			nearest = enemy
			nearest_distance = distance

	if nearest == null:
		return

	var enemy_health: int = int(nearest.get_meta("health", 100)) - BULLET_DAMAGE
	nearest.set_meta("health", enemy_health)
	wanted = min(5, wanted + 1)
	if enemy_health <= 0:
		nearest.set_meta("active", false)
		nearest.visible = false
		if mission_state == "active":
			mission_state = "return"
			status_label.text = "Alvo neutralizado. Volte ao xerife."

func _update_enemies() -> void:
	for enemy in enemies:
		if not is_instance_valid(enemy) or not enemy.get_meta("active", false):
			continue
		var distance := enemy.position.distance_to(player.position)
		if distance < 16.0 and distance > 2.5:
			var direction := enemy.position.direction_to(player.position)
			enemy.position += direction * 1.5 * get_physics_process_delta_time()
		elif distance <= 2.5:
			health = max(0, health - ENEMY_DAMAGE)

func _build_hud() -> void:
	var layer := CanvasLayer.new()
	layer.name = "HUD"
	add_child(layer)

	hud_label = Label.new()
	hud_label.position = Vector2(24, 20)
	hud_label.add_theme_font_size_override("font_size", 22)
	layer.add_child(hud_label)

	objective_label = Label.new()
	objective_label.position = Vector2(24, 65)
	objective_label.add_theme_font_size_override("font_size", 20)
	layer.add_child(objective_label)

	status_label = Label.new()
	status_label.position = Vector2(24, 105)
	status_label.add_theme_font_size_override("font_size", 18)
	layer.add_child(status_label)

func _update_hud() -> void:
	if hud_label == null:
		return
	hud_label.text = "Vida: %d   Dinheiro: $%d   Procurado: %d/5" % [health, money, wanted]
	if mission_state == "not_started":
		objective_label.text = "Objetivo: fale com o xerife em Dusty Creek"
	elif mission_state == "active":
		objective_label.text = "Objetivo: encontre o procurado"
	elif mission_state == "return":
		objective_label.text = "Objetivo: volte ao xerife"
	else:
		objective_label.text = "Objetivo: explore o Oeste"
