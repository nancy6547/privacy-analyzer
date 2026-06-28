import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import tensorflow as tf
import json

# -----------------------------------------------
# STEP 1 — CREATE TRAINING DATA
# -----------------------------------------------
# Each app has these features:
# 1. num_permissions     — total permissions requested
# 2. has_location        — uses location? (1=yes, 0=no)
# 3. has_camera          — uses camera?
# 4. has_microphone      — uses microphone?
# 5. has_contacts        — reads contacts?
# 6. has_sms             — reads SMS?
# 7. has_background_data — runs in background?
# 8. has_storage         — accesses storage?
#
# Label:
# 0 = Low Risk
# 1 = Medium Risk
# 2 = High Risk

np.random.seed(42)
n = 1000  # 1000 sample apps

data = []

for i in range(n):
    r = np.random.rand()

    if r < 0.4:
        # LOW RISK app — few permissions
        num_perm      = np.random.randint(1, 4)
        location      = 0
        camera        = np.random.randint(0, 2)
        microphone    = 0
        contacts      = 0
        sms           = 0
        background    = 0
        storage       = np.random.randint(0, 2)
        label         = 0

    elif r < 0.7:
        # MEDIUM RISK app
        num_perm      = np.random.randint(3, 7)
        location      = np.random.randint(0, 2)
        camera        = np.random.randint(0, 2)
        microphone    = np.random.randint(0, 2)
        contacts      = np.random.randint(0, 2)
        sms           = 0
        background    = np.random.randint(0, 2)
        storage       = np.random.randint(0, 2)
        label         = 1

    else:
        # HIGH RISK app — many sensitive permissions
        num_perm      = np.random.randint(6, 12)
        location      = 1
        camera        = 1
        microphone    = 1
        contacts      = 1
        sms           = np.random.randint(0, 2)
        background    = 1
        storage       = 1
        label         = 2

    data.append([num_perm, location, camera, microphone,
                 contacts, sms, background, storage, label])

df = pd.DataFrame(data, columns=[
    'num_permissions', 'has_location', 'has_camera',
    'has_microphone', 'has_contacts', 'has_sms',
    'has_background_data', 'has_storage', 'label'
])

print("Dataset created:")
print(df['label'].value_counts())
print()

# -----------------------------------------------
# STEP 2 — PREPARE DATA
# -----------------------------------------------
X = df.drop('label', axis=1).values
y = df['label'].values

# Split into train and test
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# Scale features — neural networks work better with normalized data
scaler = StandardScaler()
X_train = scaler.fit_transform(X_train)
X_test  = scaler.transform(X_test)

# Save scaler values — needed later in Android app
scaler_params = {
    'mean': scaler.mean_.tolist(),
    'scale': scaler.scale_.tolist()
}
with open('scaler_params.json', 'w') as f:
    json.dump(scaler_params, f)
print("Scaler params saved to scaler_params.json")

# -----------------------------------------------
# STEP 3 — BUILD NEURAL NETWORK
# -----------------------------------------------
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(8,)),           # 8 input features
    tf.keras.layers.Dense(16, activation='relu'),# hidden layer 1
    tf.keras.layers.Dense(8,  activation='relu'),# hidden layer 2
    tf.keras.layers.Dense(3,  activation='softmax') # output: 3 classes
])

model.compile(
    optimizer='adam',
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

model.summary()

# -----------------------------------------------
# STEP 4 — TRAIN
# -----------------------------------------------
print("\nTraining model...")
history = model.fit(
    X_train, y_train,
    epochs=50,
    batch_size=32,
    validation_split=0.1,
    verbose=1
)

# -----------------------------------------------
# STEP 5 — EVALUATE
# -----------------------------------------------
loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"\nTest Accuracy: {accuracy * 100:.2f}%")

# -----------------------------------------------
# STEP 6 — CONVERT TO TFLITE
# -----------------------------------------------
print("\nConverting to TensorFlow Lite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open('privacy_model.tflite', 'wb') as f:
    f.write(tflite_model)

print("Model saved as privacy_model.tflite")
print("Done! Two files created:")
print("  1. privacy_model.tflite  — goes into Android app")
print("  2. scaler_params.json    — goes into Android app")
