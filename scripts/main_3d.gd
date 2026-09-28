extends Node3D

# Henry Western - 3D foundation
# Offline single-player architecture. Systems are intentionally separated so
# terrain, player, horse, combat, NPCs, missions and shops can grow independently.

const WORLD_SIZE := Vector2(160.0, 110.0)
const PLAYER_SPEED := 6.0
const HORSE_SPEED := 10.0

var player: CharacterBody3D
var camera: Camera3D
var horse_position := Vector3(12.0, 0.8, -6.0)
var money := 25
var health := 100
var wanted := 0
var mounted := false
var mission_state := "not_started"

func _ready() -> void:
	player = $Player
	camera = $Camera3D
	_build_placeholder_world()
	_update_camera()

func _physics_process(delta: float) -> void:
	if player == null:
		return

	var input_2d := Input.get_vector("move_left", "move_right", "move_up", "move_down")
	var direction := Vector3(input_2d.x, 0.0, input_2d.y)
	var speed := HORSE_SPEED if mounted else PLAYER_SPEED
	player.velocity = direction * speed
	player.move_and_slide()
	player.position.x = clamp(player.position.x, -WORLD_SIZE.x * 0.5, WORLD_SIZE.x * 0.5)
	player.position.z = clamp(player.position.z, -WORLD_SIZE.y * 0.5, WORLD_SIZE.y * 0.5)
	_update_camera()

func _update_camera() -> void:
	camera.position = player.position + Vector3(0.0, 7.5, 11.0)
	camera.look_at(player.position + Vector3(0.0, 0.8, 0.0))

func _build_placeholder_world() -> void:
	# Temporary original placeholders establish the 3D gameplay scale without
	# importing copyrighted assets. Detailed western meshes will replace these.
	_create_box(Vector3(8, 3, 6), Vector3(-28, 1.5, -12), Color(0.38, 0.20, 0.10), "DustyCreekSaloon")
	_create_box(Vector3(6, 4, 6), Vector3(-18, 2, -12), Color(0.48, 0.28, 0.12), "DustyCreekStore")
	_create_box(Vector3(10, 3, 8), Vector3(20, 1.5, 24), Color(0.32, 0.18, 0.09), "RanchoRedRock")
	_create_box(Vector3(7, 5, 7), Vector3(48, 2.5, 34), Color(0.20, 0.20, 0.18), "BlackRidgeMine")
	_create_box(Vector3(5, 2, 5), Vector3(35, 1, -30), Color(0.26, 0.12, 0.08), "BanditCamp")
	_create_marker(Vector3(-8, 0.15, -4), Color(0.85, 0.75, 0.25), "Sheriff")
	_create_marker(horse_position, Color(0.28, 0.16, 0.08), "Horse")
	_create_marker(Vector3(8, 0.15, -4), Color(0.65, 0.18, 0.12), "MissionTarget")

func _create_box(size: Vector3, position: Vector3, color: Color, node_name: String) -> void:
	var mesh_instance := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = size
	mesh_instance.mesh = mesh
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	material.roughness = 0.9
	mesh_instance.material_override = material
	mesh_instance.position = position
	mesh_instance.name = node_name
	add_child(mesh_instance)

func _create_marker(position: Vector3, color: Color, node_name: String) -> void:
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
