import torch
import torch.nn as nn
import joblib
import numpy as np
import pandas as pd
import itertools
from tqdm import tqdm

class PVSelfConsumptionNet(nn.Module):
    def __init__(self, input_size=4, hidden_sizes=[64, 32, 16], dropout_rate=0.2):
        super(PVSelfConsumptionNet, self).__init__()
        layers = []
        prev_size = input_size
        
        for hidden_size in hidden_sizes:
            layers.extend([
                nn.Linear(prev_size, hidden_size),
                nn.BatchNorm1d(hidden_size),
                nn.ReLU(),
                nn.Dropout(dropout_rate)
            ])
            prev_size = hidden_size
        
        layers.append(nn.Linear(prev_size, 1))
        self.network = nn.Sequential(*layers)
        
    def forward(self, x):
        return self.network(x)

def load_model_and_scaler():
    """Load the trained model and scaler"""
    model = PVSelfConsumptionNet()
    model.load_state_dict(torch.load('final_pv_model.pth', map_location='cpu'))
    model.eval()
    
    scaler = joblib.load('feature_scaler.pkl')
    
    return model, scaler

def predict_batch_self_consumption(model, scaler, occupancy_days_list, consumption_list, pv_generation_list, battery_size_list):
    """
    Predict self-consumption for a batch of inputs
    """
    # Normalize occupancy days by 5 working days
    occupancy_normalized = [days / 5.0 for days in occupancy_days_list]
    
    # Prepare features matrix
    features = np.array([occupancy_normalized, consumption_list, pv_generation_list, battery_size_list]).T
    
    # Scale features
    features_scaled = scaler.transform(features)
    
    # Make predictions
    with torch.no_grad():
        features_tensor = torch.FloatTensor(features_scaled)
        predictions = model(features_tensor).numpy().flatten()
    
    return predictions

def generate_comprehensive_synthetic_dataset():
    """
    Generate comprehensive synthetic dataset with all parameter combinations
    """
    print("🔮 Generating Comprehensive Synthetic PV Dataset")
    print("="*60)
    
    # Define parameter ranges
    pv_sizes = list(range(1500, 20001, 100))  # 1500 to 20,000 kWh, step 100
    battery_sizes = list(range(0, 21, 1))      # 0 to 20 kWh, step 1
    consumptions = list(range(1500, 20001, 500))  # 1500 to 20,000 kWh, step 500
    occupancy_days = list(range(1, 6))         # 1 to 5 days
    
    print(f"📊 Parameter Ranges:")
    print(f"   PV Generation: {len(pv_sizes):,} values ({min(pv_sizes):,} to {max(pv_sizes):,} kWh)")
    print(f"   Battery Size: {len(battery_sizes):,} values ({min(battery_sizes)} to {max(battery_sizes)} kWh)")
    print(f"   Annual Consumption: {len(consumptions):,} values ({min(consumptions):,} to {max(consumptions):,} kWh)")
    print(f"   Occupancy Days: {len(occupancy_days)} values ({min(occupancy_days)} to {max(occupancy_days)} days)")
    
    # Calculate total combinations
    total_combinations = len(pv_sizes) * len(battery_sizes) * len(consumptions) * len(occupancy_days)
    print(f"\n🔢 Total Combinations: {total_combinations:,}")
    
    # Confirm before proceeding
    proceed = input(f"\nGenerate {total_combinations:,} combinations? This may take several minutes. (y/n): ")
    if proceed.lower() != 'y':
        print("❌ Generation cancelled")
        return
    
    # Load model and scaler
    print("\n🤖 Loading trained model...")
    model, scaler = load_model_and_scaler()
    
    # Generate all combinations
    print("🔄 Generating combinations...")
    combinations = list(itertools.product(occupancy_days, consumptions, pv_sizes, battery_sizes))
    
    print(f"✅ Generated {len(combinations):,} combinations")
    
    # Process in batches for efficiency
    batch_size = 10000
    all_results = []
    
    print("🧠 Predicting self-consumption for all combinations...")
    
    for i in tqdm(range(0, len(combinations), batch_size), desc="Processing batches"):
        batch = combinations[i:i + batch_size]
        
        # Separate batch into individual lists
        batch_occupancy = [combo[0] for combo in batch]
        batch_consumption = [combo[1] for combo in batch]
        batch_pv = [combo[2] for combo in batch]
        batch_battery = [combo[3] for combo in batch]
        
        # Predict for this batch
        predictions = predict_batch_self_consumption(
            model, scaler, batch_occupancy, batch_consumption, batch_pv, batch_battery
        )
        
        # Store results
        for j, (occ, cons, pv, batt) in enumerate(batch):
            result = {
                'occupancy_days': occ,
                'occupancy_days_normalized': occ / 5.0,
                'annual_consumption_kwh': cons,
                'pv_generation_kwh': pv,
                'battery_size_kwh': batt,
                'predicted_self_consumption_percentage': predictions[j]
            }
            all_results.append(result)
    
    # Create DataFrame
    print("📊 Creating DataFrame...")
    df = pd.DataFrame(all_results)
    
    # Add some derived columns for analysis
    df['pv_to_consumption_ratio'] = df['pv_generation_kwh'] / df['annual_consumption_kwh']
    df['battery_to_consumption_ratio'] = (df['battery_size_kwh'] * 365) / df['annual_consumption_kwh']  # Approximate daily cycles
    
    # Sort by self-consumption percentage for easier analysis
    df = df.sort_values('predicted_self_consumption_percentage', ascending=False)
    
    # Save to CSV
    filename = 'comprehensive_pv_synthetic_dataset.csv'
    print(f"💾 Saving to {filename}...")
    df.to_csv(filename, index=False)
    
    # Display summary statistics
    print(f"\n📈 Dataset Summary:")
    print(f"   Total Records: {len(df):,}")
    print(f"   File Size: ~{len(df) * 6 * 8 / 1024 / 1024:.1f} MB (estimated)")
    print(f"\n📊 Self-Consumption Statistics:")
    print(df['predicted_self_consumption_percentage'].describe())
    
    print(f"\n🎯 Top 10 Highest Self-Consumption Scenarios:")
    top_10 = df.head(10)[['occupancy_days', 'annual_consumption_kwh', 'pv_generation_kwh', 
                          'battery_size_kwh', 'predicted_self_consumption_percentage']]
    print(top_10.to_string(index=False))
    
    print(f"\n🔻 Bottom 10 Lowest Self-Consumption Scenarios:")
    bottom_10 = df.tail(10)[['occupancy_days', 'annual_consumption_kwh', 'pv_generation_kwh', 
                             'battery_size_kwh', 'predicted_self_consumption_percentage']]
    print(bottom_10.to_string(index=False))
    
    print(f"\n✅ Dataset generation complete!")
    print(f"📁 Saved to: {filename}")
    
    return df

def analyze_dataset_patterns(df):
    """
    Analyze patterns in the generated dataset
    """
    print("\n🔍 Dataset Analysis:")
    print("="*40)
    
    # Occupancy impact
    print("📊 Average Self-Consumption by Occupancy Days:")
    occupancy_avg = df.groupby('occupancy_days')['predicted_self_consumption_percentage'].mean()
    for days, avg in occupancy_avg.items():
        print(f"   {days} days: {avg:.1f}%")
    
    # Battery impact
    print("\n🔋 Average Self-Consumption by Battery Size:")
    battery_ranges = [(0, 0), (1, 5), (6, 10), (11, 15), (16, 20)]
    for min_batt, max_batt in battery_ranges:
        mask = (df['battery_size_kwh'] >= min_batt) & (df['battery_size_kwh'] <= max_batt)
        avg = df[mask]['predicted_self_consumption_percentage'].mean()
        print(f"   {min_batt}-{max_batt} kWh: {avg:.1f}%")
    
    # PV/Consumption ratio impact
    print("\n⚡ Average Self-Consumption by PV/Consumption Ratio:")
    df['pv_ratio_bin'] = pd.cut(df['pv_to_consumption_ratio'], bins=5, labels=['Very Low', 'Low', 'Medium', 'High', 'Very High'])
    ratio_avg = df.groupby('pv_ratio_bin')['predicted_self_consumption_percentage'].mean()
    for ratio, avg in ratio_avg.items():
        print(f"   {ratio}: {avg:.1f}%")

if __name__ == "__main__":
    try:
        # Generate the comprehensive dataset
        df = generate_comprehensive_synthetic_dataset()
        
        # Analyze patterns
        if df is not None:
            analyze_dataset_patterns(df)
            
    except FileNotFoundError as e:
        print(f"❌ Error: {e}")
        print("Make sure you have run the neural network training first to generate the model files.")
    except KeyboardInterrupt:
        print("\n❌ Generation cancelled by user")
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc() 