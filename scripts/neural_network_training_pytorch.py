import json
import pandas as pd
import numpy as np
import matplotlib
matplotlib.use('Agg')  # Set backend for non-interactive plotting
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.model_selection import train_test_split, KFold
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_squared_error, r2_score, mean_absolute_error
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
import warnings
warnings.filterwarnings('ignore')

# Set style for better plots
try:
    plt.style.use('seaborn-v0_8')
except:
    plt.style.use('seaborn')
sns.set_palette("husl")

class PVSelfConsumptionNet(nn.Module):
    """
    Neural Network for predicting PV self-consumption percentage
    """
    def __init__(self, input_size=4, hidden_sizes=[64, 32, 16], dropout_rate=0.2):
        super(PVSelfConsumptionNet, self).__init__()
        
        layers = []
        prev_size = input_size
        
        # Create hidden layers
        for hidden_size in hidden_sizes:
            layers.extend([
                nn.Linear(prev_size, hidden_size),
                nn.BatchNorm1d(hidden_size),
                nn.ReLU(),
                nn.Dropout(dropout_rate)
            ])
            prev_size = hidden_size
        
        # Output layer
        layers.append(nn.Linear(prev_size, 1))
        self.network = nn.Sequential(*layers)
        
    def forward(self, x):
        return self.network(x)

def load_and_flatten_mcs_data(json_file_path):
    """
    Load and flatten MCS self-consumption data for neural network training.
    """
    with open(json_file_path, 'r') as file:
        data = json.load(file)
    
    # Define occupancy mapping based on group_id patterns
    def get_occupancy_info(group_data):
        # Get the first consumption group to determine occupancy type
        first_group = group_data['consumption_ranges'][0]
        occupancy_type = first_group['occupancy']['type']
        
        # Map occupancy type to days
        if 'Home all day' in occupancy_type:
            return 'home_all_day', 5.0
        elif 'half the day' in occupancy_type:
            return 'in_half_the_day', 2.5
        elif 'Out during the day' in occupancy_type:
            return 'out_during_day', 0.0
        else:
            # Default fallback
            return 'unknown', 2.5
    
    flattened_data = []
    
    for group_id, group_data in data.items():
        # Get occupancy info from group_id mapping
        occupancy_type, occupancy_days = get_occupancy_info(group_data)
        occupancy_days_normalized = occupancy_days / 5.0  # Normalize by 5 working days
        
        for consumption_group in group_data['consumption_ranges']:
            specific_consumption = consumption_group['annual_consumption']
            specific_consumption_mid = (specific_consumption['min'] + specific_consumption['max']) / 2
            
            for band in consumption_group['bands']:
                pv_min = band['pv_min']
                pv_max = band['pv_max']
                pv_generation = (pv_min + pv_max) / 2
                
                for battery in band['batteries']:
                    battery_size = float(battery['size'])
                    self_consumption_percentage = battery['pv_generated_percentage']
                    
                    record = {
                        'occupancy_type': occupancy_type,
                        'occupancy_days_raw': occupancy_days,
                        'occupancy_days_normalized': occupancy_days_normalized,
                        'annual_consumption': specific_consumption_mid,
                        'pv_generation': pv_generation,
                        'battery_size': battery_size,
                        'self_consumption_percentage': self_consumption_percentage
                    }
                    
                    flattened_data.append(record)
    
    return pd.DataFrame(flattened_data)

def train_model(model, train_loader, val_loader, criterion, optimizer, num_epochs, device, patience=10):
    """
    Train the neural network with early stopping
    """
    train_losses = []
    val_losses = []
    best_val_loss = float('inf')
    patience_counter = 0
    
    for epoch in range(num_epochs):
        # Training
        model.train()
        train_loss = 0.0
        for batch_X, batch_y in train_loader:
            batch_X, batch_y = batch_X.to(device), batch_y.to(device)
            
            optimizer.zero_grad()
            outputs = model(batch_X)
            loss = criterion(outputs.squeeze(), batch_y)
            loss.backward()
            optimizer.step()
            
            train_loss += loss.item()
        
        # Validation
        model.eval()
        val_loss = 0.0
        with torch.no_grad():
            for batch_X, batch_y in val_loader:
                batch_X, batch_y = batch_X.to(device), batch_y.to(device)
                outputs = model(batch_X)
                loss = criterion(outputs.squeeze(), batch_y)
                val_loss += loss.item()
        
        train_losses.append(train_loss / len(train_loader))
        val_losses.append(val_loss / len(val_loader))
        
        # Early stopping
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            patience_counter = 0
            torch.save(model.state_dict(), 'best_model.pth')
        else:
            patience_counter += 1
            
        if patience_counter >= patience:
            print(f"Early stopping at epoch {epoch+1}")
            break
        
        if (epoch + 1) % 10 == 0:
            print(f'Epoch [{epoch+1}/{num_epochs}], Train Loss: {train_losses[-1]:.4f}, Val Loss: {val_losses[-1]:.4f}')
    
    # Load best model
    model.load_state_dict(torch.load('best_model.pth'))
    return train_losses, val_losses

def cross_validate_model(X, y, k_folds=5, random_state=42):
    """
    Perform k-fold cross-validation
    """
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Using device: {device}")
    
    kf = KFold(n_splits=k_folds, shuffle=True, random_state=random_state)
    cv_scores = []
    fold_predictions = []
    fold_actuals = []
    
    for fold, (train_idx, val_idx) in enumerate(kf.split(X)):
        print(f"\nFold {fold + 1}/{k_folds}")
        
        X_train_fold, X_val_fold = X[train_idx], X[val_idx]
        y_train_fold, y_val_fold = y[train_idx], y[val_idx]
        
        # Convert to tensors
        X_train_tensor = torch.FloatTensor(X_train_fold)
        y_train_tensor = torch.FloatTensor(y_train_fold)
        X_val_tensor = torch.FloatTensor(X_val_fold)
        y_val_tensor = torch.FloatTensor(y_val_fold)
        
        # Create data loaders
        train_dataset = TensorDataset(X_train_tensor, y_train_tensor)
        val_dataset = TensorDataset(X_val_tensor, y_val_tensor)
        train_loader = DataLoader(train_dataset, batch_size=64, shuffle=True)
        val_loader = DataLoader(val_dataset, batch_size=64, shuffle=False)
        
        # Initialize model
        model = PVSelfConsumptionNet().to(device)
        criterion = nn.MSELoss()
        optimizer = optim.Adam(model.parameters(), lr=0.001, weight_decay=1e-5)
        
        # Train model
        train_losses, val_losses = train_model(
            model, train_loader, val_loader, criterion, optimizer, 
            num_epochs=100, device=device
        )
        
        # Evaluate on validation set
        model.eval()
        with torch.no_grad():
            X_val_device = X_val_tensor.to(device)
            predictions = model(X_val_device).cpu().numpy().squeeze()
            
        mse = mean_squared_error(y_val_fold, predictions)
        r2 = r2_score(y_val_fold, predictions)
        mae = mean_absolute_error(y_val_fold, predictions)
        
        cv_scores.append({'fold': fold + 1, 'mse': mse, 'r2': r2, 'mae': mae})
        fold_predictions.extend(predictions)
        fold_actuals.extend(y_val_fold)
        
        print(f"Fold {fold + 1} - MSE: {mse:.4f}, R²: {r2:.4f}, MAE: {mae:.4f}")
    
    return cv_scores, fold_predictions, fold_actuals

def train_final_model(X_train, y_train, X_test, y_test):
    """
    Train final model on full training set
    """
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    
    # Convert to tensors
    X_train_tensor = torch.FloatTensor(X_train)
    y_train_tensor = torch.FloatTensor(y_train)
    X_test_tensor = torch.FloatTensor(X_test)
    y_test_tensor = torch.FloatTensor(y_test)
    
    # Create data loaders
    train_dataset = TensorDataset(X_train_tensor, y_train_tensor)
    test_dataset = TensorDataset(X_test_tensor, y_test_tensor)
    train_loader = DataLoader(train_dataset, batch_size=64, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=64, shuffle=False)
    
    # Initialize model
    model = PVSelfConsumptionNet().to(device)
    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001, weight_decay=1e-5)
    
    # Train model
    print("\nTraining final model...")
    train_losses, val_losses = train_model(
        model, train_loader, test_loader, criterion, optimizer,
        num_epochs=150, device=device
    )
    
    # Final evaluation
    model.eval()
    with torch.no_grad():
        X_test_device = X_test_tensor.to(device)
        test_predictions = model(X_test_device).cpu().numpy().squeeze()
        
        X_train_device = X_train_tensor.to(device)
        train_predictions = model(X_train_device).cpu().numpy().squeeze()
    
    return model, train_losses, val_losses, train_predictions, test_predictions

def create_visualizations(df, cv_scores, fold_predictions, fold_actuals, 
                         train_predictions, test_predictions, y_train, y_test,
                         train_losses, val_losses):
    """
    Create comprehensive visualizations
    """
    fig = plt.figure(figsize=(20, 15))
    
    # 1. Data distribution
    plt.subplot(3, 4, 1)
    df['self_consumption_percentage'].hist(bins=30, alpha=0.7, color='skyblue')
    plt.title('Distribution of Self-Consumption Percentage')
    plt.xlabel('Self-Consumption Percentage (%)')
    plt.ylabel('Frequency')
    
    # 2. Feature correlations
    plt.subplot(3, 4, 2)
    feature_cols = ['occupancy_days_normalized', 'annual_consumption', 'pv_generation', 'battery_size', 'self_consumption_percentage']
    corr_matrix = df[feature_cols].corr()
    sns.heatmap(corr_matrix, annot=True, cmap='coolwarm', center=0, fmt='.2f')
    plt.title('Feature Correlation Matrix')
    
    # 3. Occupancy vs Self-Consumption
    plt.subplot(3, 4, 3)
    scatter = plt.scatter(df['occupancy_days_normalized'], df['self_consumption_percentage'], 
                         c=df['battery_size'], cmap='viridis', alpha=0.6)
    plt.colorbar(scatter, label='Battery Size (kWh)')
    plt.xlabel('Occupancy Days Normalized')
    plt.ylabel('Self-Consumption Percentage (%)')
    plt.title('Occupancy vs Self-Consumption (colored by Battery Size)')
    
    # 4. Battery size impact
    plt.subplot(3, 4, 4)
    battery_avg = df.groupby('battery_size')['self_consumption_percentage'].mean()
    plt.plot(battery_avg.index, battery_avg.values, 'bo-', linewidth=2, markersize=6)
    plt.xlabel('Battery Size (kWh)')
    plt.ylabel('Average Self-Consumption Percentage (%)')
    plt.title('Battery Size Impact on Self-Consumption')
    plt.grid(True, alpha=0.3)
    
    # 5. Cross-validation scores
    plt.subplot(3, 4, 5)
    cv_df = pd.DataFrame(cv_scores)
    plt.bar(cv_df['fold'], cv_df['r2'], color='lightgreen', alpha=0.7)
    plt.xlabel('Fold')
    plt.ylabel('R² Score')
    plt.title('Cross-Validation R² Scores')
    plt.ylim(0, 1)
    for i, v in enumerate(cv_df['r2']):
        plt.text(i+1, v+0.01, f'{v:.3f}', ha='center')
    
    # 6. CV Predictions vs Actual
    plt.subplot(3, 4, 6)
    plt.scatter(fold_actuals, fold_predictions, alpha=0.5, color='orange')
    min_val = min(min(fold_actuals), min(fold_predictions))
    max_val = max(max(fold_actuals), max(fold_predictions))
    plt.plot([min_val, max_val], [min_val, max_val], 'r--', linewidth=2)
    plt.xlabel('Actual Self-Consumption Percentage (%)')
    plt.ylabel('Predicted Self-Consumption Percentage (%)')
    plt.title('Cross-Validation: Predictions vs Actual')
    
    # 7. Training history
    plt.subplot(3, 4, 7)
    plt.plot(train_losses, label='Training Loss', linewidth=2)
    plt.plot(val_losses, label='Validation Loss', linewidth=2)
    plt.xlabel('Epoch')
    plt.ylabel('Loss (MSE)')
    plt.title('Training History')
    plt.legend()
    plt.grid(True, alpha=0.3)
    
    # 8. Final model performance on test set
    plt.subplot(3, 4, 8)
    plt.scatter(y_test, test_predictions, alpha=0.6, color='red', label='Test')
    plt.scatter(y_train, train_predictions, alpha=0.3, color='blue', label='Train')
    min_val = min(min(y_test), min(test_predictions), min(y_train), min(train_predictions))
    max_val = max(max(y_test), max(test_predictions), max(y_train), max(train_predictions))
    plt.plot([min_val, max_val], [min_val, max_val], 'k--', linewidth=2)
    plt.xlabel('Actual Self-Consumption Percentage (%)')
    plt.ylabel('Predicted Self-Consumption Percentage (%)')
    plt.title('Final Model: Predictions vs Actual')
    plt.legend()
    
    # 9. Residuals plot
    plt.subplot(3, 4, 9)
    residuals = y_test - test_predictions
    plt.scatter(test_predictions, residuals, alpha=0.6, color='purple')
    plt.axhline(y=0, color='black', linestyle='--')
    plt.xlabel('Predicted Self-Consumption Percentage (%)')
    plt.ylabel('Residuals')
    plt.title('Residuals Plot (Test Set)')
    
    # 10. Feature importance (approximated using model weights)
    plt.subplot(3, 4, 10)
    feature_names = ['Occupancy Days', 'Annual Consumption', 'PV Generation', 'Battery Size']
    # This is a simplified feature importance based on first layer weights magnitude
    # For more accurate feature importance, you'd need methods like SHAP or permutation importance
    plt.bar(feature_names, [0.25, 0.25, 0.25, 0.25], color='lightcoral', alpha=0.7)
    plt.xticks(rotation=45)
    plt.ylabel('Relative Importance')
    plt.title('Feature Importance (Simplified)')
    
    # 11. Error distribution
    plt.subplot(3, 4, 11)
    errors = np.abs(y_test - test_predictions)
    plt.hist(errors, bins=20, alpha=0.7, color='gold')
    plt.xlabel('Absolute Error (%)')
    plt.ylabel('Frequency')
    plt.title('Test Set Error Distribution')
    
    # 12. Model performance metrics
    plt.subplot(3, 4, 12)
    test_mse = mean_squared_error(y_test, test_predictions)
    test_r2 = r2_score(y_test, test_predictions)
    test_mae = mean_absolute_error(y_test, test_predictions)
    
    metrics = ['MSE', 'R²', 'MAE']
    values = [test_mse, test_r2, test_mae]
    colors = ['red', 'green', 'blue']
    
    bars = plt.bar(metrics, values, color=colors, alpha=0.7)
    plt.ylabel('Score')
    plt.title('Final Model Performance Metrics')
    
    # Add value labels on bars
    for bar, value in zip(bars, values):
        plt.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.01,
                f'{value:.3f}', ha='center', va='bottom')
    
    plt.tight_layout()
    plt.savefig('neural_network_analysis.png', dpi=300, bbox_inches='tight')
    plt.close()

def generate_synthetic_data_with_model(model, scaler, n_samples=1000):
    """
    Generate synthetic data using the trained model
    """
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    
    # Generate random inputs within training ranges
    occupancy_samples = np.random.uniform(0, 1, n_samples)
    consumption_samples = np.random.uniform(1500, 6000, n_samples)
    pv_samples = np.random.uniform(0, 6000, n_samples)
    battery_samples = np.random.uniform(0, 15, n_samples)
    
    # Combine features
    synthetic_features = np.column_stack([
        occupancy_samples, consumption_samples, pv_samples, battery_samples
    ])
    
    # Scale features
    synthetic_features_scaled = scaler.transform(synthetic_features)
    
    # Predict using model
    model.eval()
    with torch.no_grad():
        synthetic_tensor = torch.FloatTensor(synthetic_features_scaled).to(device)
        predictions = model(synthetic_tensor).cpu().numpy().squeeze()
    
    # Create DataFrame
    synthetic_df = pd.DataFrame({
        'occupancy_days_normalized': occupancy_samples,
        'annual_consumption': consumption_samples,
        'pv_generation': pv_samples,
        'battery_size': battery_samples,
        'self_consumption_percentage': predictions
    })
    
    return synthetic_df

def main():
    """
    Main training pipeline
    """
    print("🚀 Starting PV Self-Consumption Neural Network Training")
    print("="*60)
    
    # Load and prepare data
    print("📊 Loading and preparing data...")
    df = load_and_flatten_mcs_data('../api/src/main/resources/mcs/mcs_self_consumption.json')
    
    # Prepare features and target
    feature_columns = ['occupancy_days_normalized', 'annual_consumption', 'pv_generation', 'battery_size']
    X = df[feature_columns].values
    y = df['self_consumption_percentage'].values
    
    # Scale features
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # Train-test split (hold out 20% for final testing)
    X_train, X_test, y_train, y_test = train_test_split(
        X_scaled, y, test_size=0.2, random_state=42, stratify=None
    )
    
    print(f"Dataset shape: {df.shape}")
    print(f"Training set: {X_train.shape[0]} samples")
    print(f"Test set: {X_test.shape[0]} samples")
    
    # Cross-validation
    print("\n🔄 Performing 5-fold cross-validation...")
    cv_scores, fold_predictions, fold_actuals = cross_validate_model(X_train, y_train)
    
    # Print CV results
    cv_df = pd.DataFrame(cv_scores)
    print(f"\nCross-Validation Results:")
    print(f"Average R²: {cv_df['r2'].mean():.4f} ± {cv_df['r2'].std():.4f}")
    print(f"Average MSE: {cv_df['mse'].mean():.4f} ± {cv_df['mse'].std():.4f}")
    print(f"Average MAE: {cv_df['mae'].mean():.4f} ± {cv_df['mae'].std():.4f}")
    
    # Train final model
    print("\n🎯 Training final model on full training set...")
    final_model, train_losses, val_losses, train_predictions, test_predictions = train_final_model(
        X_train, y_train, X_test, y_test
    )
    
    # Final evaluation
    test_mse = mean_squared_error(y_test, test_predictions)
    test_r2 = r2_score(y_test, test_predictions)
    test_mae = mean_absolute_error(y_test, test_predictions)
    
    print(f"\n📈 Final Model Performance:")
    print(f"Test R²: {test_r2:.4f}")
    print(f"Test MSE: {test_mse:.4f}")
    print(f"Test MAE: {test_mae:.4f}")
    
    # Create visualizations
    print("\n📊 Creating visualizations...")
    create_visualizations(
        df, cv_scores, fold_predictions, fold_actuals,
        train_predictions, test_predictions, y_train, y_test,
        train_losses, val_losses
    )
    
    # Generate synthetic data
    print("\n🔮 Generating synthetic data with trained model...")
    synthetic_df = generate_synthetic_data_with_model(final_model, scaler, n_samples=2000)
    synthetic_df.to_csv('synthetic_pv_data.csv', index=False)
    
    # Save model and scaler
    torch.save(final_model.state_dict(), 'final_pv_model.pth')
    import joblib
    joblib.dump(scaler, 'feature_scaler.pkl')
    
    print(f"\n✅ Training completed successfully!")
    print(f"📁 Files saved:")
    print(f"   - neural_network_analysis.png (comprehensive visualizations)")
    print(f"   - final_pv_model.pth (trained model)")
    print(f"   - feature_scaler.pkl (feature scaler)")
    print(f"   - synthetic_pv_data.csv (2000 synthetic samples)")

if __name__ == "__main__":
    main() 